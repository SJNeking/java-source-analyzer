import { parse as parseBabel, ParserOptions } from '@babel/parser';
import traverse, { NodePath } from '@babel/traverse';
import * as t from '@babel/types';

/**
 * Babel-based parser for JSX, Vue SFC, and modern JS features
 * Complements the TypeScript Compiler API parser
 */
export class BabelParser {
  private ast: t.File | null = null;

  parse(sourceCode: string, filePath: string): t.File {
    const options: ParserOptions = {
      sourceType: 'module',
      plugins: [
        'typescript',
        'jsx',
        'classProperties',
        'decorators-legacy',
        'dynamicImport',
        'exportDefaultFrom',
        'exportNamespaceFrom',
        'nullishCoalescingOperator',
        'optionalChaining',
        'bigInt',
      ],
      sourceFilename: filePath,
    };

    this.ast = parseBabel(sourceCode, options);
    return this.ast;
  }

  getAST(): t.File | null {
    return this.ast;
  }

  /**
   * Extract React component definitions
   */
  extractReactComponents(ast: t.File): ReactComponentInfo[] {
    const components: ReactComponentInfo[] = [];

    traverse(ast, {
      // Function components
      FunctionDeclaration(path) {
        const node = path.node;
        if (node.id && isReactComponentFunction(path)) {
          components.push({
            name: node.id.name,
            type: 'function',
            line: node.loc?.start.line || 0,
            endLine: node.loc?.end.line || 0,
            params: node.params.map((p) => formatParam(p)),
            hooksUsed: extractHooksFromFunction(path),
            jsxElements: countJSXElements(path),
            propsInterface: extractPropsInterface(path),
          });
        }
      },

      // Arrow function components
      VariableDeclaration(path) {
        path.node.declarations.forEach((decl) => {
          if (
            t.isIdentifier(decl.id) &&
            t.isArrowFunctionExpression(decl.init) &&
            isReactComponentArrow(path)
          ) {
            const hooks = extractHooksFromFunction(path as any);
            components.push({
              name: decl.id.name,
              type: 'arrow',
              line: decl.init.loc?.start.line || 0,
              endLine: decl.init.loc?.end.line || 0,
              params: decl.init.params.map((p) => formatParam(p)),
              hooksUsed: hooks,
              jsxElements: countJSXElementsInArrow(decl.init),
              propsInterface: extractPropsFromArrow(decl.init),
            });
          }
        });
      },

      // Class components
      ClassDeclaration(path) {
        const node = path.node;
        if (
          node.id &&
          node.superClass &&
          t.isMemberExpression(node.superClass) &&
          t.isIdentifier(node.superClass.object) &&
          node.superClass.object.name === 'React'
        ) {
          const methods: string[] = [];
          const stateProperties: string[] = [];

          path.get('body').get('body').forEach((memberPath) => {
            if (memberPath.isClassMethod()) {
              methods.push((memberPath.node.key as t.Identifier).name);
            }
            if (memberPath.isClassProperty()) {
              const key = (memberPath.node.key as t.Identifier)?.name;
              if (key) stateProperties.push(key);
            }
          });

          components.push({
            name: node.id.name,
            type: 'class',
            line: node.loc?.start.line || 0,
            endLine: node.loc?.end.line || 0,
            params: [],
            hooksUsed: [],
            jsxElements: 0,
            propsInterface: [],
            methods,
            stateProperties,
          });
        }
      },
    });

    return components;
  }

  /**
   * Extract Vue SFC component information
   */
  extractVueComponent(sourceCode: string, filePath: string): VueComponentInfo | null {
    // Vue SFC parsing requires @vue/compiler-sfc which is heavy
    // Use regex-based extraction for the template and script sections
    const scriptMatch = sourceCode.match(/<script(?:\s+[^>]*)?>([\s\S]*?)<\/script>/);
    const templateMatch = sourceCode.match(/<template(?:\s+[^>]*)?>([\s\S]*?)<\/template>/);
    const styleMatch = sourceCode.match(/<style(?:\s+[^>]*)?>([\s\S]*?)<\/style>/);

    if (!scriptMatch) return null;

    const scriptContent = scriptMatch[1];
    const isCompositionAPI = scriptContent.includes('setup(') || scriptContent.includes('defineComponent');
    const hasPropsDefinition =
      scriptContent.includes('props:') ||
      scriptContent.includes('defineProps(') ||
      scriptContent.includes('prop:');
    const hasEmitsDefinition =
      scriptContent.includes('emits:') ||
      scriptContent.includes('defineEmits(') ||
      scriptContent.includes('$emit');

    return {
      filePath,
      line: scriptMatch.index ? sourceCode.substring(0, scriptMatch.index).split('\n').length : 0,
      isCompositionAPI,
      hasPropsDefinition,
      hasEmitsDefinition,
      hasTemplate: !!templateMatch,
      hasStyle: !!styleMatch,
      scriptContent,
      templateContent: templateMatch ? templateMatch[1] : '',
    };
  }

  /**
   * Extract useEffect/useLayoutEffect calls with dependency analysis
   */
  extractEffectCalls(ast: t.File): EffectCallInfo[] {
    const effects: EffectCallInfo[] = [];

    traverse(ast, {
      CallExpression(path) {
        const callee = path.node.callee;
        if (
          t.isIdentifier(callee) &&
          (callee.name === 'useEffect' || callee.name === 'useLayoutEffect')
        ) {
          const args = path.node.arguments;
          const hasDependencyArray = args.length >= 2 && t.isArrayExpression(args[1]);
          const deps =
            hasDependencyArray && t.isArrayExpression(args[1])
              ? args[1].elements.map((el) => {
                  if (t.isIdentifier(el)) return el.name;
                  if (t.isMemberExpression(el) && t.isIdentifier(el.object) && t.isIdentifier(el.property))
                    return `${el.object.name}.${el.property.name}`;
                  return '<complex>';
                })
              : [];

          const hasCleanupFunction =
            args.length >= 1 &&
            t.isArrowFunctionExpression(args[0]) &&
            t.isBlockStatement(args[0].body) &&
            hasReturnFunction(args[0].body);

          effects.push({
            type: callee.name,
            line: path.node.loc?.start.line || 0,
            hasDependencyArray,
            dependencies: deps,
            isEmptyDeps: hasDependencyArray && deps.length === 0,
            hasCleanupFunction,
          });
        }
      },
    });

    return effects;
  }

  /**
   * Extract event handler patterns (onXXX props)
   */
  extractEventHandlers(ast: t.File): EventHandlerInfo[] {
    const handlers: EventHandlerInfo[] = [];

    traverse(ast, {
      JSXAttribute(path) {
        const name = path.node.name;
        if (t.isJSXIdentifier(name) && name.name.startsWith('on')) {
          const value = path.node.value;
          const isInlineHandler =
            t.isJSXExpressionContainer(value) &&
            (t.isArrowFunctionExpression(value.expression) ||
             t.isFunctionExpression(value.expression));

          handlers.push({
            eventName: name.name,
            line: path.node.loc?.start.line || 0,
            isInlineHandler,
          });
        }
      },
    });

    return handlers;
  }
}

// ==================== Type Definitions ====================

export interface ReactComponentInfo {
  name: string;
  type: 'function' | 'arrow' | 'class';
  line: number;
  endLine: number;
  params: string[];
  hooksUsed: string[];
  jsxElements: number;
  propsInterface: string[];
  methods?: string[];
  stateProperties?: string[];
}

export interface VueComponentInfo {
  filePath: string;
  line: number;
  isCompositionAPI: boolean;
  hasPropsDefinition: boolean;
  hasEmitsDefinition: boolean;
  hasTemplate: boolean;
  hasStyle: boolean;
  scriptContent: string;
  templateContent: string;
}

export interface EffectCallInfo {
  type: string;
  line: number;
  hasDependencyArray: boolean;
  dependencies: string[];
  isEmptyDeps: boolean;
  hasCleanupFunction: boolean;
}

export interface EventHandlerInfo {
  eventName: string;
  line: number;
  isInlineHandler: boolean;
}

// ==================== Helper Functions ====================

function isReactComponentFunction(path: NodePath<t.FunctionDeclaration>): boolean {
  const body = path.node.body;
  if (!t.isBlockStatement(body)) return false;

  // Check if it returns JSX
  let returnsJSX = false;
  traverse(path.node.body, {
    ReturnStatement(retPath) {
      if (t.isJSXElement(retPath.node.argument) || t.isJSXFragment(retPath.node.argument)) {
        returnsJSX = true;
        retPath.stop();
      }
    },
    noScope: true,
  });

  return returnsJSX;
}

function isReactComponentArrow(path: NodePath<t.VariableDeclaration>): boolean {
  // Check if it returns JSX
  let returnsJSX = false;
  path.traverse({
    ReturnStatement(retPath) {
      if (t.isJSXElement(retPath.node.argument) || t.isJSXFragment(retPath.node.argument)) {
        returnsJSX = true;
        retPath.stop();
      }
    },
    noScope: true,
  });

  return returnsJSX;
}

function extractHooksFromFunction(path: NodePath<any>): string[] {
  const hooks: string[] = [];

  path.traverse({
    CallExpression(callPath) {
      const callee = callPath.node.callee;
      if (t.isIdentifier(callee) && callee.name.match(/^(use[A-Z]|use\w+)/)) {
        hooks.push(callee.name);
      }
    },
    noScope: true,
  });

  return [...new Set(hooks)];
}

function countJSXElements(path: NodePath<any>): number {
  let count = 0;

  path.traverse({
    JSXElement() {
      count++;
    },
    JSXFragment() {
      count++;
    },
    noScope: true,
  });

  return count;
}

function countJSXElementsInArrow(node: t.ArrowFunctionExpression): number {
  let count = 0;

  traverse(node, {
    JSXElement() {
      count++;
    },
    JSXFragment() {
      count++;
    },
    noScope: true,
  });

  return count;
}

function extractPropsInterface(path: NodePath<any>): string[] {
  const props: string[] = [];

  // Check for TypeScript interface Props { ... }
  const bindings = path.scope.getAllBindings();
  Object.values(bindings).forEach((binding) => {
    if (binding.identifier.typeAnnotation) {
      props.push(binding.identifier.name);
    }
  });

  return props;
}

function extractPropsFromArrow(node: t.ArrowFunctionExpression): string[] {
  const props: string[] = [];

  if (node.params.length > 0 && t.isIdentifier(node.params[0])) {
    props.push(node.params[0].name);
  }

  return props;
}

function formatParam(param: t.Identifier | t.RestElement | t.Pattern): string {
  if (t.isIdentifier(param)) return param.name;
  if (t.isRestElement(param) && t.isIdentifier(param.argument)) return `...${param.argument.name}`;
  return '<destructured>';
}

function hasReturnFunction(node: t.BlockStatement): boolean {
  let hasReturn = false;

  traverse(node, {
    ReturnStatement() {
      hasReturn = true;
    },
    noScope: true,
  });

  return hasReturn;
}
