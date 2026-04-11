import * as ts from 'typescript';
import { ASTParser } from '../types';

/**
 * TypeScript AST Parser using the official TypeScript Compiler API
 * Provides real syntactic and semantic analysis (not regex-based)
 */
export class TypeScriptParser implements ASTParser {
  private sourceFile: ts.SourceFile | null = null;
  private program: ts.Program | null = null;
  private checker: ts.TypeChecker | null = null;

  parse(sourceCode: string, filePath: string): ts.SourceFile {
    this.sourceFile = ts.createSourceFile(
      filePath,
      sourceCode,
      ts.ScriptTarget.Latest,
      /* setParentNodes */ true
    );

    // Create a minimal program for type checking
    const compilerOptions: ts.CompilerOptions = {
      target: ts.ScriptTarget.ES2020,
      module: ts.ModuleKind.CommonJS,
      strict: true,
      esModuleInterop: true,
      skipLibCheck: true,
      noEmit: true,
    };

    // Create a language service host for type checking
    const host = ts.createCompilerHost(compilerOptions);
    host.getSourceFile = (fileName) => {
      if (fileName === filePath) {
        return this.sourceFile!;
      }
      return ts.createSourceFile(fileName, '', ts.ScriptTarget.Latest);
    };
    host.readFile = (fileName) => {
      if (fileName === filePath) return sourceCode;
      return '';
    };
    host.fileExists = () => true;
    host.getDefaultLibFileName = () => 'lib.d.ts';
    host.writeFile = () => {};
    host.getCurrentDirectory = () => process.cwd();
    host.getCanonicalFileName = (f) => f;
    host.useCaseSensitiveFileNames = () => true;
    host.getNewLine = () => '\n';

    this.program = ts.createProgram([filePath], compilerOptions, host);
    this.checker = this.program.getTypeChecker();

    return this.sourceFile;
  }

  getLanguage(): string {
    return 'typescript';
  }

  getChecker(): ts.TypeChecker | null {
    return this.checker;
  }

  getProgram(): ts.Program | null {
    return this.program;
  }

  /**
   * Extract all functions/methods from the AST
   */
  extractFunctions(sourceFile: ts.SourceFile): FunctionInfo[] {
    const functions: FunctionInfo[] = [];

    const visit = (node: ts.Node) => {
      if (
        ts.isFunctionDeclaration(node) ||
        ts.isMethodDeclaration(node) ||
        ts.isArrowFunction(node) ||
        ts.isFunctionExpression(node)
      ) {
        const info = this.extractFunctionInfo(node as ts.FunctionLikeDeclaration, sourceFile);
        if (info) functions.push(info);
      }
      ts.forEachChild(node, visit);
    };

    visit(sourceFile);
    return functions;
  }

  /**
   * Extract all classes from the AST
   */
  extractClasses(sourceFile: ts.SourceFile): ClassInfo[] {
    const classes: ClassInfo[] = [];

    const visit = (node: ts.Node) => {
      if (ts.isClassDeclaration(node)) {
        const className = node.name?.text || '<anonymous>';
        const methods: string[] = [];
        const properties: PropertyInfo[] = [];

        node.members.forEach((member) => {
          if (ts.isMethodDeclaration(member)) {
            const methodName = member.name.getText(sourceFile);
            methods.push(methodName);
          }
          if (ts.isPropertyDeclaration(member)) {
            const propInfo = this.extractPropertyInfo(member, sourceFile);
            if (propInfo) properties.push(propInfo);
          }
        });

        const decorators = this.extractDecorators(node);
        const extendsClause = node.heritageClauses?.find(
          (c) => c.token === ts.SyntaxKind.ExtendsKeyword
        );
        const implementsClause = node.heritageClauses?.find(
          (c) => c.token === ts.SyntaxKind.ImplementsKeyword
        );

        classes.push({
          name: className,
          filePath: sourceFile.fileName,
          line: sourceFile.getLineAndCharacterOfPosition(node.getStart()).line + 1,
          methods,
          properties,
          decorators,
          extends: extendsClause?.types.map((t) => t.expression.getText(sourceFile)) || [],
          implements: implementsClause?.types.map((t) => t.expression.getText(sourceFile)) || [],
        });
      }
      ts.forEachChild(node, visit);
    };

    visit(sourceFile);
    return classes;
  }

  /**
   * Extract all imports from the AST
   */
  extractImports(sourceFile: ts.SourceFile): ImportInfo[] {
    const imports: ImportInfo[] = [];

    ts.forEachChild(sourceFile, (node) => {
      if (ts.isImportDeclaration(node)) {
        const moduleSpecifier = node.moduleSpecifier.getText(sourceFile).replace(/['"]/g, '');
        const importClause = node.importClause;

        let defaultImport: string | undefined;
        const namedImports: string[] = [];
        const namespaceImport: string | undefined = undefined;

        if (importClause) {
          if (importClause.name) {
            defaultImport = importClause.name.text;
          }
          if (importClause.namedBindings) {
            if (ts.isNamedImports(importClause.namedBindings)) {
              importClause.namedBindings.elements.forEach((el) => {
                namedImports.push(el.name.text);
              });
            }
            if (ts.isNamespaceImport(importClause.namedBindings)) {
              // @ts-ignore
              namespaceImport = importClause.namedBindings.name.text;
            }
          }
        }

        const isTypeOnly = importClause?.isTypeOnly || false;

        imports.push({
          moduleSpecifier,
          defaultImport,
          namedImports,
          namespaceImport,
          isTypeOnly,
          line: sourceFile.getLineAndCharacterOfPosition(node.getStart()).line + 1,
        });
      }
    });

    return imports;
  }

  /**
   * Count `any` type usage
   */
  countAnyTypeUsage(sourceFile: ts.SourceFile): AnyUsageInfo {
    const result: AnyUsageInfo = {
      implicitAny: [],
      explicitAny: [],
      total: 0,
    };

    const visit = (node: ts.Node) => {
      // Check explicit `any`
      if (node.kind === ts.SyntaxKind.AnyKeyword) {
        const pos = sourceFile.getLineAndCharacterOfPosition(node.getStart());
        result.explicitAny.push({
          line: pos.line + 1,
          column: pos.character + 1,
        });
        result.total++;
      }

      // Check implicit any (parameters without type annotations)
      if (ts.isParameter(node) && !node.type) {
        // Check if it's a function parameter without type
        const parent = node.parent;
        if (
          ts.isFunctionDeclaration(parent) ||
          ts.isMethodDeclaration(parent) ||
          ts.isArrowFunction(parent) ||
          ts.isFunctionExpression(parent)
        ) {
          // Only report if it's not a destructuring pattern
          if (ts.isIdentifier(node.name)) {
            const pos = sourceFile.getLineAndCharacterOfPosition(node.getStart());
            result.implicitAny.push({
              name: node.name.text,
              line: pos.line + 1,
              column: pos.character + 1,
            });
            result.total++;
          }
        }
      }

      ts.forEachChild(node, visit);
    };

    visit(sourceFile);
    return result;
  }

  /**
   * Extract cyclomatic complexity estimation
   */
  calculateCyclomaticComplexity(node: ts.Node): number {
    let complexity = 1; // Base complexity

    const visit = (n: ts.Node) => {
      if (
        ts.isIfStatement(n) ||
        ts.isConditionalExpression(n) ||
        ts.isWhileStatement(n) ||
        ts.isDoStatement(n) ||
        ts.isForStatement(n) ||
        ts.isForInStatement(n) ||
        ts.isForOfStatement(n) ||
        ts.isCaseClause(n) ||
        ts.isCatchClause(n)
      ) {
        complexity++;
      }

      // Logical operators add to complexity
      if (ts.isBinaryExpression(n)) {
        if (
          n.operatorToken.kind === ts.SyntaxKind.AmpersandAmpersandToken ||
          n.operatorToken.kind === ts.SyntaxKind.BarBarToken
        ) {
          complexity++;
        }
      }

      ts.forEachChild(n, visit);
    };

    visit(node);
    return complexity;
  }

  /**
   * Get type annotation for a node
   */
  getTypeAnnotation(node: ts.Node): string | undefined {
    if (this.checker) {
      const type = this.checker.getTypeAtLocation(node);
      return this.checker.typeToString(type);
    }
    return undefined;
  }

  /**
   * Check if function has explicit return type
   */
  hasExplicitReturnType(func: ts.FunctionLikeDeclaration): boolean {
    return func.type !== undefined;
  }

  /**
   * Extract decorators from a node
   */
  private extractDecorators(node: ts.Node): string[] {
    const decorators: string[] = [];
    
    // Check if node has decorators property
    if ('decorators' in node && Array.isArray(node.decorators) && node.decorators.length > 0) {
      node.decorators.forEach((d) => {
        if (ts.isCallExpression(d.expression)) {
          decorators.push(d.expression.expression.getText(this.sourceFile!));
        } else {
          decorators.push(d.expression.getText(this.sourceFile!));
        }
      });
    }
    
    return decorators;
  }

  private extractFunctionInfo(
    node: ts.FunctionLikeDeclaration,
    sourceFile: ts.SourceFile
  ): FunctionInfo | null {
    const name =
      ts.isFunctionDeclaration(node)
        ? node.name?.text || '<anonymous>'
        : ts.isMethodDeclaration(node)
        ? node.name.getText(sourceFile)
        : '<anonymous>';

    const pos = sourceFile.getLineAndCharacterOfPosition(node.getStart());
    const end = sourceFile.getLineAndCharacterOfPosition(node.getEnd());

    const body = node.body;
    let bodyCode = '';
    if (body) {
      if (ts.isBlock(body)) {
        bodyCode = body.getText(sourceFile);
      }
    }

    const params: ParameterInfo[] = [];
    if (node.parameters) {
      node.parameters.forEach((p) => {
        const paramInfo: ParameterInfo = {
          name: ts.isIdentifier(p.name) ? p.name.text : '<destructured>',
          hasType: p.type !== undefined,
          type: p.type ? p.type.getText(sourceFile) : 'implicit any',
        };
        params.push(paramInfo);
      });
    }

    const hasReturnType = node.type !== undefined;
    const returnType = node.type ? node.type.getText(sourceFile) : undefined;

    const complexity = body ? this.calculateCyclomaticComplexity(body) : 1;

    return {
      name,
      filePath: sourceFile.fileName,
      line: pos.line + 1,
      endLine: end.line + 1,
      params,
      hasReturnType,
      returnType,
      bodyCode,
      complexity,
      lineCount: end.line - pos.line + 1,
    };
  }

  private extractPropertyInfo(
    node: ts.PropertyDeclaration,
    sourceFile: ts.SourceFile
  ): PropertyInfo | null {
    const name = node.name.getText(sourceFile);
    const type = node.type ? node.type.getText(sourceFile) : 'implicit any';
    const modifiers = (node.modifiers || []).map((m) =>
      ts.tokenToString(m.kind)
    ).filter(Boolean) as string[];
    const pos = sourceFile.getLineAndCharacterOfPosition(node.getStart());

    return {
      name,
      type,
      modifiers,
      line: pos.line + 1,
    };
  }
}

// ==================== Type Definitions ====================

export interface FunctionInfo {
  name: string;
  filePath: string;
  line: number;
  endLine: number;
  params: ParameterInfo[];
  hasReturnType: boolean;
  returnType?: string;
  bodyCode: string;
  complexity: number;
  lineCount: number;
}

export interface ParameterInfo {
  name: string;
  hasType: boolean;
  type: string;
}

export interface ClassInfo {
  name: string;
  filePath: string;
  line: number;
  methods: string[];
  properties: PropertyInfo[];
  decorators: string[];
  extends: string[];
  implements: string[];
}

export interface PropertyInfo {
  name: string;
  type: string;
  modifiers: string[];
  line: number;
}

export interface ImportInfo {
  moduleSpecifier: string;
  defaultImport?: string;
  namedImports: string[];
  namespaceImport?: string;
  isTypeOnly: boolean;
  line: number;
}

export interface AnyUsageInfo {
  implicitAny: Array<{ name: string; line: number; column: number }>;
  explicitAny: Array<{ line: number; column: number }>;
  total: number;
}
