package cn.dolphinmind.glossary.java.analyze.quality;

import cn.dolphinmind.glossary.java.analyze.config.RulesConfig;
import cn.dolphinmind.glossary.java.analyze.quality.rules.*;

import java.util.*;

/**
 * Central registry for all quality rules.
 *
 * Replaces the 450+ lines of manual reg.accept() calls in SourceUniversePro.
 * Rules are organized by category and statically registered.
 *
 * Usage:
 *   RuleRegistry.registerAll(engine, rulesConfig);
 *   // or selectively:
 *   RuleRegistry.getBugRules().forEach(engine::registerRule);
 */
public final class RuleRegistry {
    private RuleRegistry() {}

    // =====================================================================
    // BUG RULES (from AllRules)
    // =====================================================================
    private static final List<QualityRule> BUG_RULES = Collections.unmodifiableList(Arrays.asList(
            new AllRules.EmptyCatchBlock(),
            new AllRules.StringLiteralEquality(),
            new AllRules.IdenticalOperand(),
            new AllRules.ThreadRunDirect(),
            new AllRules.WaitNotifyNoSync(),
            new AllRules.MutableMembersReturned(),
            new AllRules.FinalizerUsed(),
            new AllRules.MissingSerialVersionUID(),
            new AllRules.NullDereference(),
            new AllRules.DeadStore(),
            new AllRules.AssertSideEffect(),
            new AllRules.LoopBranchUpdate(),
            new AllRules.PublicStaticMutableField(),
            new AllRules.DeprecatedUsage(),
            new AllRules.EqualsOnArrays(),
            new AllRules.BigDecimalDouble(),
            new AllRules.ToStringReturnsNull(),
            new AllRules.ClassLoaderMisuse(),
            new AllRules.ExceptionRethrown(),
            new AllRules.UncheckedCatch(),
            new AllRules.UnclosedResource(),
            new AllRules.InterruptedExceptionSwallowed(),
            new AllRules.LongToIntCast()
    ));

    // =====================================================================
    // CODE_SMELL RULES (from AllRules)
    // =====================================================================
    private static final List<QualityRule> CODE_SMELL_RULES = Collections.unmodifiableList(Arrays.asList(
            new AllRules.TooLongMethod(),
            new AllRules.TooManyParameters(),
            new AllRules.TooManyReturns(),
            new AllRules.CyclomaticComplexity(),
            new AllRules.PrintStackTrace(),
            new AllRules.GodClass(),
            new AllRules.MissingJavadoc(),
            new AllRules.WildcardImport(),
            new AllRules.SystemOutPrintln(),
            new AllRules.TooManyConstructors(),
            new AllRules.EmptyStatement(),
            new AllRules.UnusedLocalVariable(),
            new AllRules.TooManyStringLiterals(),
            new AllRules.ExceptionIgnored(),
            new AllRules.EqualsWithoutHashCode(),
            new AllRules.StringConcatInLoop(),
            new AllRules.EmptyMethodBody(),
            new AllRules.BooleanLiteralInCondition(),
            new AllRules.StringEqualsCaseSensitive(),
            new AllRules.SensitiveToString(),
            new AllRules.BooleanMethodName(),
            new AllRules.MethodTooLongName(),
            new AllRules.ThreadSleepInCode(),
            new AllRules.MagicNumber(),
            new AllRules.RedundantCast(),
            new AllRules.SerializableField(),
            new AllRules.VariableDeclaredFarFromUsage(),
            new AllRules.BigDecimalPrecisionLoss()
    ));

    // =====================================================================
    // SECURITY RULES (from AllRules)
    // =====================================================================
    private static final List<QualityRule> SECURITY_RULES = Collections.unmodifiableList(Arrays.asList(
            new AllRules.DOMParserXXE(),
            new AllRules.OptionalParameter(),
            new AllRules.OptionalGetWithoutCheck(),
            new AllRules.StreamNotConsumed(),
            new AllRules.OptionalField(),
            new AllRules.SSLServerSocket(),
            new AllRules.WeakRSAKey(),
            new AllRules.AllocationInLoop(),
            new AllRules.CatchingError(),
            new AllRules.OptionalChaining(),
            new AllRules.TLSProtocol(),
            new AllRules.AutoboxingPerformance(),
            new AllRules.WeakHashFunction(),
            new AllRules.JWTWithoutExpiry(),
            new AllRules.RegexComplexity(),
            new AllRules.RegexLookaround(),
            new AllRules.RegexDoS(),
            new AllRules.InsecureRandom(),
            new AllRules.NullCheckAfterDeref(),
            new AllRules.LogInjection(),
            new AllRules.HashWithoutSalt(),
            new AllRules.InsecureCookie(),
            new AllRules.HttpOnlyCookie(),
            new AllRules.FilePermissionTooPermissive(),
            new AllRules.ContentTypeSniffing(),
            new AllRules.HardcodedPassword(),
            new AllRules.SQLInjection(),
            new AllRules.HardcodedIP(),
            new AllRules.HTTPNotHTTPS(),
            new AllRules.InsecureRandomGenerator(),
            new AllRules.Deserialization(),
            new AllRules.CommandInjection(),
            new AllRules.WeakMAC(),
            new AllRules.ReflectionOnSensitive(),
            new AllRules.HardcodedSecretKey(),
            new AllRules.SessionFixation(),
            new AllRules.InsecureTempFile(),
            new AllRules.LDAPInjection(),
            new AllRules.PathTraversal(),
            new AllRules.OpenRedirect(),
            new AllRules.XXEInTransformerFactory(),
            new AllRules.XPathInjection(),
            new AllRules.CORSMisconfiguration()
    ));

    // =====================================================================
    // CFG / ADVANCED RULES
    // =====================================================================
    public static List<QualityRule> getCfgRules(RulesConfig rulesConfig) {
        List<QualityRule> rules = new ArrayList<>();
        int ccThreshold = rulesConfig.getEffectiveThreshold("RSPEC-3776-CFG", 15);
        rules.add(new TrueCyclomaticComplexity(ccThreshold));
        rules.add(new UnreachableCode());
        rules.add(new ResourceLeakPath());
        rules.add(new ExceptionHandlingPath());
        rules.add(new TaintFlowRule());
        rules.add(new EnhancedTaintFlowRule());
        return Collections.unmodifiableList(rules);
    }

    // =====================================================================
    // OWASP TOP 10
    // =====================================================================
    private static final List<QualityRule> OWASP_RULES = Collections.unmodifiableList(Arrays.asList(
            new OwaspTop10Rules.AccessControlNotImplemented(),
            new OwaspTop10Rules.InsecureJWTValidation(),
            new OwaspTop10Rules.TLSVerificationDisabled(),
            new OwaspTop10Rules.WeakCryptoAlgorithm(),
            new OwaspTop10Rules.XXEInjection(),
            new OwaspTop10Rules.OpenRedirect(),
            new OwaspTop10Rules.UnrestrictedFileUpload(),
            new OwaspTop10Rules.SecurityMisconfiguration(),
            new OwaspTop10Rules.DeprecatedInsecureAPI(),
            new OwaspTop10Rules.PasswordStoredInCode(),
            new OwaspTop10Rules.PlaintextPassword(),
            new OwaspTop10Rules.InsecureDeserialization(),
            new OwaspTop10Rules.SensitiveInfoLogged(),
            new OwaspTop10Rules.ExceptionMessageExposed(),
            new OwaspTop10Rules.SSRF()
    ));

    // =====================================================================
    // PERFORMANCE RULES
    // =====================================================================
    private static final List<QualityRule> PERFORMANCE_RULES = Collections.unmodifiableList(Arrays.asList(
            new PerformanceRules.NPlusOneQuery(),
            new PerformanceRules.MissingBatchOperation(),
            new PerformanceRules.InefficientCollectionInit(),
            new PerformanceRules.InefficientLoopCondition(),
            new PerformanceRules.UnboundedStaticCollection(),
            new PerformanceRules.ResourceNotClosed(),
            new PerformanceRules.ThreadPoolMisconfiguration(),
            new PerformanceRules.ConnectionNotClosed(),
            new PerformanceRules.StringConcatInLoopEnhanced()
    ));

    // =====================================================================
    // ARCHITECTURE RULES
    // =====================================================================
    private static final List<QualityRule> ARCHITECTURE_RULES = Collections.unmodifiableList(Arrays.asList(
            new ArchitectureRules.LayerViolation(),
            new ArchitectureRules.ExcessiveFanOut(),
            new ArchitectureRules.FeatureEnvy(),
            new ArchitectureRules.GodClassEnhanced(),
            new ArchitectureRules.DataClass(),
            new ArchitectureRules.UnstableDependency(),
            new ArchitectureRules.MissingAbstraction()
    ));

    // =====================================================================
    // MAINTAINABILITY RULES
    // =====================================================================
    private static final List<QualityRule> MAINTAINABILITY_RULES = Collections.unmodifiableList(Arrays.asList(
            new MaintainabilityRules.DuplicateCodeBlock(),
            new MaintainabilityRules.ClassNameMismatch(),
            new MaintainabilityRules.ShortVariableName(),
            new MaintainabilityRules.MisleadingComment(),
            new MaintainabilityRules.CommentedOutCode(),
            new MaintainabilityRules.GenericExceptionCaught(),
            new MaintainabilityRules.ExceptionSwallowed(),
            new MaintainabilityRules.InefficientLogging(),
            new MaintainabilityRules.LoggingLevelMismatch(),
            new MaintainabilityRules.UnreachableCode(),
            new MaintainabilityRules.MagicNumber()
    ));

    // =====================================================================
    // CONCURRENCY RULES
    // =====================================================================
    private static final List<QualityRule> CONCURRENCY_RULES = Collections.unmodifiableList(Arrays.asList(
            new ConcurrencyRules.SynchronizedOnPrimitive(),
            new ConcurrencyRules.StaticMutableNotThreadSafe(),
            new ConcurrencyRules.InconsistentLockOrdering(),
            new ConcurrencyRules.LockNotReleased(),
            new ConcurrencyRules.CheckThenActRace(),
            new ConcurrencyRules.DoubleCheckedLocking(),
            new ConcurrencyRules.SharedFieldWithoutVolatile(),
            new ConcurrencyRules.NonAtomicCompoundAction(),
            new ConcurrencyRules.UnsafeLazyInit(),
            new ConcurrencyRules.UnboundedThreadPool(),
            new ConcurrencyRules.ThreadSleepInCode()
    ));

    // =====================================================================
    // JAVA MODERNIZATION RULES
    // =====================================================================
    private static final List<QualityRule> MODERNIZATION_RULES = Collections.unmodifiableList(Arrays.asList(
            new JavaModernizationRules.UseLocalDate(),
            new JavaModernizationRules.LegacyCollections(),
            new JavaModernizationRules.StringBufferInsteadOfBuilder(),
            new JavaModernizationRules.UseTryWithResources(),
            new JavaModernizationRules.AnonymousClassToLambda(),
            new JavaModernizationRules.StringLengthCheck(),
            new JavaModernizationRules.ManualStringJoin(),
            new JavaModernizationRules.WrapperClassConstructor(),
            new JavaModernizationRules.UnnecessaryBoxing(),
            new JavaModernizationRules.RedundantUnboxing()
    ));

    // =====================================================================
    // DATABASE RULES
    // =====================================================================
    private static final List<QualityRule> DATABASE_RULES = Collections.unmodifiableList(Arrays.asList(
            new DatabaseRules.HardcodedDatabaseCredentials(),
            new DatabaseRules.SelectStarUsage(),
            new DatabaseRules.SqlInLoop(),
            new DatabaseRules.ResultSetNotClosed(),
            new DatabaseRules.StatementNotClosed(),
            new DatabaseRules.ConnectionNotClosed(),
            new DatabaseRules.EntityManagerNotClosed(),
            new DatabaseRules.TransactionNotCommitted()
    ));

    // =====================================================================
    // SPRING BOOT RULES
    // =====================================================================
    private static final List<QualityRule> SPRING_BOOT_RULES = Collections.unmodifiableList(Arrays.asList(
            new SpringBootRules.MissingRestController(),
            new SpringBootRules.RequestMappingWithoutMethod(),
            new SpringBootRules.GenericExceptionHandler(),
            new SpringBootRules.RedundantAutowired(),
            new SpringBootRules.TransactionalOnPrivate(),
            new SpringBootRules.ConfigurationNotFinal(),
            new SpringBootRules.MissingConditionalOnProperty(),
            new SpringBootRules.MissingPathVariable(),
            new SpringBootRules.MissingRequestParamRequired(),
            new SpringBootRules.BeanWithoutName(),
            new SpringBootRules.DefaultComponentScan()
    ));

    // =====================================================================
    // ROBUSTNESS RULES
    // =====================================================================
    private static final List<QualityRule> ROBUSTNESS_RULES = Collections.unmodifiableList(Arrays.asList(
            new RobustnessRules.SwitchMissingDefault(),
            new RobustnessRules.EmptyFinallyBlock(),
            new RobustnessRules.SunApiUsage(),
            new RobustnessRules.PublicField(),
            new RobustnessRules.DoubleBraceInitialization(),
            new RobustnessRules.StaticInitializerException(),
            new RobustnessRules.BooleanInversion(),
            new RobustnessRules.UtilityClassPattern(),
            new RobustnessRules.CloneMisuse(),
            new RobustnessRules.ReadResolveWrongType()
    ));

    // =====================================================================
    // WEB API RULES
    // =====================================================================
    private static final List<QualityRule> WEB_API_RULES = Collections.unmodifiableList(Arrays.asList(
            new WebApiRules.MissingHttpMethod(),
            new WebApiRules.MissingResponseStatusCode(),
            new WebApiRules.MissingCorsHeaders(),
            new WebApiRules.PathVariableInjection(),
            new WebApiRules.MissingPagination(),
            new WebApiRules.MissingInputValidation(),
            new WebApiRules.SensitiveDataExposure(),
            new WebApiRules.MissingContentType(),
            new WebApiRules.MissingRateLimiting(),
            new WebApiRules.ErrorDetailsExposed()
    ));

    // =====================================================================
    // MICROSERVICE RULES
    // =====================================================================
    private static final List<QualityRule> MICROSERVICE_RULES = Collections.unmodifiableList(Arrays.asList(
            new MicroserviceRules.MissingCircuitBreaker(),
            new MicroserviceRules.MissingTimeout(),
            new MicroserviceRules.MissingRetryLogic(),
            new MicroserviceRules.HardcodedServiceUrl(),
            new MicroserviceRules.MissingFallback(),
            new MicroserviceRules.SyncCriticalPath(),
            new MicroserviceRules.MissingLoadBalancing()
    ));

    // =====================================================================
    // TEST QUALITY RULES
    // =====================================================================
    private static final List<QualityRule> TEST_QUALITY_RULES = Collections.unmodifiableList(Arrays.asList(
            new TestQualityRules.TestWithoutAssertion(),
            new TestQualityRules.TestMethodNameConvention(),
            new TestQualityRules.ExcessiveMocking(),
            new TestQualityRules.TestCodeDuplication(),
            new TestQualityRules.FlakyTest(),
            new TestQualityRules.TestWithoutCleanup(),
            new TestQualityRules.TestCatchGenericException(),
            new TestQualityRules.TestOrderDependency(),
            new TestQualityRules.MissingParameterizedTest()
    ));

    // =====================================================================
    // INPUT VALIDATION RULES
    // =====================================================================
    private static final List<QualityRule> INPUT_VALIDATION_RULES = Collections.unmodifiableList(Arrays.asList(
            new InputValidationRules.MissingNullCheck(),
            new InputValidationRules.MissingRangeValidation(),
            new InputValidationRules.TrustingExternalInput(),
            new InputValidationRules.MissingFormatValidation(),
            new InputValidationRules.MissingSizeValidation(),
            new InputValidationRules.MissingTypeValidation(),
            new InputValidationRules.MissingAllowlist(),
            new InputValidationRules.MissingEncodingValidation(),
            new InputValidationRules.MissingBoundaryCheck(),
            new InputValidationRules.MissingInputSanitization()
    ));

    // =====================================================================
    // CODE SMELL ENHANCED RULES
    // =====================================================================
    private static final List<QualityRule> CODE_SMELL_ENHANCED_RULES = Collections.unmodifiableList(Arrays.asList(
            new CodeSmellEnhancedRules.GodMethod(),
            new CodeSmellEnhancedRules.SpeculativeGenerality(),
            new CodeSmellEnhancedRules.DeadCode(),
            new CodeSmellEnhancedRules.LongParameterList(),
            new CodeSmellEnhancedRules.DataClumps(),
            new CodeSmellEnhancedRules.ShotgunSurgery(),
            new CodeSmellEnhancedRules.MessageChains(),
            new CodeSmellEnhancedRules.MiddleMan(),
            new CodeSmellEnhancedRules.InappropriateIntimacy(),
            new CodeSmellEnhancedRules.CommentsAsSmell()
    ));

    // =====================================================================
    // SOLID PRINCIPLES RULES
    // =====================================================================
    private static final List<QualityRule> SOLID_RULES = Collections.unmodifiableList(Arrays.asList(
            new SolidRules.SingleResponsibility(),
            new SolidRules.OpenClosedViolation(),
            new SolidRules.LiskovSubstitution(),
            new SolidRules.InterfaceSegregation(),
            new SolidRules.DependencyInversion(),
            new SolidRules.GodObject(),
            new SolidRules.FeatureEnvy(),
            new SolidRules.DivergentChange(),
            new SolidRules.DataClass()
    ));

    // =====================================================================
    // SECURITY ENHANCED RULES
    // =====================================================================
    private static final List<QualityRule> SECURITY_ENHANCED_RULES = Collections.unmodifiableList(Arrays.asList(
            new SecurityEnhancedRules.CsrfDisabled(),
            new SecurityEnhancedRules.CookieWithoutSecure(),
            new SecurityEnhancedRules.MissingCsp(),
            new SecurityEnhancedRules.InformationDisclosure(),
            new SecurityEnhancedRules.WeakHashing(),
            new SecurityEnhancedRules.InsecureFilePermissions(),
            new SecurityEnhancedRules.MissingAuthentication(),
            new SecurityEnhancedRules.InsecureRandom(),
            new SecurityEnhancedRules.HardcodedSecrets(),
            new SecurityEnhancedRules.InsecureCors(),
            new SecurityEnhancedRules.MissingHttps(),
            new SecurityEnhancedRules.InsecurePasswordStorage(),
            new SecurityEnhancedRules.MissingAuditLogging(),
            new SecurityEnhancedRules.XxeInjection(),
            new SecurityEnhancedRules.LdapInjection()
    ));

    // =====================================================================
    // RESOURCE MANAGEMENT RULES
    // =====================================================================
    private static final List<QualityRule> RESOURCE_MANAGEMENT_RULES = Collections.unmodifiableList(Arrays.asList(
            new ResourceManagementRules.StreamNotClosed(),
            new ResourceManagementRules.ScannerNotClosed(),
            new ResourceManagementRules.ThreadPoolNotShutdown(),
            new ResourceManagementRules.TimerNotCancelled(),
            new ResourceManagementRules.ProcessNotDestroyed(),
            new ResourceManagementRules.JdbcConnectionNotClosed(),
            new ResourceManagementRules.ZipFileNotClosed(),
            new ResourceManagementRules.HttpConnectionNotDisconnected(),
            new ResourceManagementRules.WriterNotFlushed(),
            new ResourceManagementRules.RandomAccessFileNotClosed()
    ));

    // =====================================================================
    // JAVA 8+ RULES
    // =====================================================================
    private static final List<QualityRule> JAVA8_PLUS_RULES = Collections.unmodifiableList(Arrays.asList(
            new Java8PlusRules.UseStreamApi(),
            new Java8PlusRules.UseOptional(),
            new Java8PlusRules.UseMethodReference(),
            new Java8PlusRules.UseStringJoin(),
            new Java8PlusRules.UseForEach(),
            new Java8PlusRules.UseCollectorsToMap(),
            new Java8PlusRules.UseTryWithResources(),
            new Java8PlusRules.UseOptionalMap(),
            new Java8PlusRules.UseArraysAsList(),
            new Java8PlusRules.UseCompletableFuture()
    ));

    // =====================================================================
    // EXCEPTION HANDLING RULES
    // =====================================================================
    private static final List<QualityRule> EXCEPTION_HANDLING_RULES = Collections.unmodifiableList(Arrays.asList(
            new ExceptionHandlingRules.LoggingAndThrowing(),
            new ExceptionHandlingRules.ReturnNullOnException(),
            new ExceptionHandlingRules.ExceptionForControlFlow(),
            new ExceptionHandlingRules.IncompleteCatch(),
            new ExceptionHandlingRules.CatchNpe(),
            new ExceptionHandlingRules.CatchIndexOutOfBounds(),
            new ExceptionHandlingRules.RuntimeExceptionInConstructor(),
            new ExceptionHandlingRules.UninformativeExceptionMessage(),
            new ExceptionHandlingRules.ExceptionWrappedWithoutCause(),
            new ExceptionHandlingRules.CatchingThrowable()
    ));

    // =====================================================================
    // LOGGING RULES
    // =====================================================================
    private static final List<QualityRule> LOGGING_RULES = Collections.unmodifiableList(Arrays.asList(
            new LoggingRules.UsingSystemOut(),
            new LoggingRules.StringConcatInLog(),
            new LoggingRules.LoggingSensitiveData(),
            new LoggingRules.ExceptionWithoutStackTrace(),
            new LoggingRules.WrongLogLevel(),
            new LoggingRules.DebugInProduction(),
            new LoggingRules.LoggingInLoop(),
            new LoggingRules.MissingLogParameter(),
            new LoggingRules.LoggerNotStaticFinal(),
            new LoggingRules.LogNotInternationalized()
    ));

    // =====================================================================
    // COLLECTION RULES
    // =====================================================================
    private static final List<QualityRule> COLLECTION_RULES = Collections.unmodifiableList(Arrays.asList(
            new CollectionRules.ArrayListAsQueue(),
            new CollectionRules.HashSetInitialCapacity(),
            new CollectionRules.HashMapInitialCapacity(),
            new CollectionRules.LinkedListRandomAccess(),
            new CollectionRules.ConcurrentModification(),
            new CollectionRules.MutableSingletonSet(),
            new CollectionRules.FixedSizeList(),
            new CollectionRules.TreeMapWhenNotNeeded(),
            new CollectionRules.ContainsOnList(),
            new CollectionRules.EnumerationInsteadOfIterator()
    ));

    // =====================================================================
    // STRING RULES
    // =====================================================================
    private static final List<QualityRule> STRING_RULES = Collections.unmodifiableList(Arrays.asList(
            new StringRules.StringConcatInLoop(),
            new StringRules.EqualsOnCharArray(),
            new StringRules.EmptyStringCheck(),
            new StringRules.StringSplitRegex(),
            new StringRules.SubstringBounds(),
            new StringRules.StringInternMisuse(),
            new StringRules.CaseInsensitiveComparison(),
            new StringRules.StringBuilderCapacity(),
            new StringRules.StringFormatVsConcatenation(),
            new StringRules.StringValueOfVsConcatenation()
    ));

    // =====================================================================
    // DESIGN PATTERN RULES
    // =====================================================================
    private static final List<QualityRule> DESIGN_PATTERN_RULES = Collections.unmodifiableList(Arrays.asList(
            new DesignPatternRules.SingletonWithPublicConstructor(),
            new DesignPatternRules.IncompleteBuilderPattern(),
            new DesignPatternRules.ComplexFactoryMethod(),
            new DesignPatternRules.MissingObserverPattern(),
            new DesignPatternRules.MissingStrategyPattern(),
            new DesignPatternRules.DecoratorAntiPattern(),
            new DesignPatternRules.TemplateMethodViolation(),
            new DesignPatternRules.VisitorMisuse(),
            new DesignPatternRules.MissingCommandPattern(),
            new DesignPatternRules.AdapterSmell()
    ));

    // =====================================================================
    // CODE ORGANIZATION RULES
    // =====================================================================
    private static final List<QualityRule> CODE_ORGANIZATION_RULES = Collections.unmodifiableList(Arrays.asList(
            new CodeOrganizationRules.WrongPackageName(),
            new CodeOrganizationRules.StaticImportMisuse(),
            new CodeOrganizationRules.UnusedImport(),
            new CodeOrganizationRules.TooManyImports(),
            new CodeOrganizationRules.DuplicateImport(),
            new CodeOrganizationRules.PackageInfoMissing(),
            new CodeOrganizationRules.WrongClassOrder(),
            new CodeOrganizationRules.WrongMethodOrder(),
            new CodeOrganizationRules.TestInWrongPackage(),
            new CodeOrganizationRules.NestedClassTooDeep()
    ));

    // =====================================================================
    // API DESIGN RULES
    // =====================================================================
    private static final List<QualityRule> API_DESIGN_RULES = Collections.unmodifiableList(Arrays.asList(
            new ApiDesignRules.BooleanParameter(),
            new ApiDesignRules.ReturnNullInsteadOfEmpty(),
            new ApiDesignRules.MutableReturnType(),
            new ApiDesignRules.MethodNameMismatch(),
            new ApiDesignRules.ApiReturnsImplementation(),
            new ApiDesignRules.SynchronizedMethod(),
            new ApiDesignRules.DeprecatedApiStillUsed(),
            new ApiDesignRules.ThrowsCheckedException(),
            new ApiDesignRules.BuilderNotUsed(),
            new ApiDesignRules.TooManyReturnPaths()
    ));

    // =====================================================================
    // REFLECTION RULES
    // =====================================================================
    private static final List<QualityRule> REFLECTION_RULES = Collections.unmodifiableList(Arrays.asList(
            new ReflectionRules.SetAccessible(),
            new ReflectionRules.MethodInvokePerformance(),
            new ReflectionRules.ReflectionFieldAccess(),
            new ReflectionRules.ClassForNameWithoutLoader(),
            new ReflectionRules.ProxyMisuse(),
            new ReflectionRules.ConstructorNewInstancePerformance(),
            new ReflectionRules.ReflectionInHotPath(),
            new ReflectionRules.DynamicClassLoading(),
            new ReflectionRules.MethodHandleNotCached(),
            new ReflectionRules.ArrayLengthCheckMissing(),
            new ReflectionRules.EnumOrdinalUsed()
    ));

    // =====================================================================
    // SECURITY & PERFORMANCE RULES (New High-Impact Rules)
    // =====================================================================
    private static final List<QualityRule> SECURITY_PERFORMANCE_RULES = Collections.unmodifiableList(Arrays.asList(
            new SecurityPerformanceRules.InsecureDirectObjectReference(),
            new SecurityPerformanceRules.MassAssignment(),
            new SecurityPerformanceRules.SimpleDateFormatThreadSafety(),
            new SecurityPerformanceRules.OptionalOrElsePerformance(),
            new SecurityPerformanceRules.JakartaMigration(),
            new SecurityPerformanceRules.ParallelStreamMisuse(),
            new SecurityPerformanceRules.StreamReuse(),
            new SecurityPerformanceRules.ImmutableCollectionModification(),
            new SecurityPerformanceRules.NoSqlInjection(),
            new SecurityPerformanceRules.VirtualThreadBlocking(),
            new SecurityPerformanceRules.MissingGracefulShutdown(),
            new SecurityPerformanceRules.ServerSideTemplateInjection(),
            new SecurityPerformanceRules.MissingIdempotency(),
            new SecurityPerformanceRules.ConcurrentHashMapBlocking()
    ));

    // =====================================================================
    // PUBLIC ACCESSORS
    // =====================================================================

    public static List<QualityRule> getBugRules() { return BUG_RULES; }
    public static List<QualityRule> getCodeSmellRules() { return CODE_SMELL_RULES; }
    public static List<QualityRule> getSecurityRules() { return SECURITY_RULES; }
    public static List<QualityRule> getOwaspRules() { return OWASP_RULES; }
    public static List<QualityRule> getPerformanceRules() { return PERFORMANCE_RULES; }
    public static List<QualityRule> getArchitectureRules() { return ARCHITECTURE_RULES; }
    public static List<QualityRule> getMaintainabilityRules() { return MAINTAINABILITY_RULES; }
    public static List<QualityRule> getConcurrencyRules() { return CONCURRENCY_RULES; }
    public static List<QualityRule> getModernizationRules() { return MODERNIZATION_RULES; }
    public static List<QualityRule> getDatabaseRules() { return DATABASE_RULES; }
    public static List<QualityRule> getSpringBootRules() { return SPRING_BOOT_RULES; }
    public static List<QualityRule> getRobustnessRules() { return ROBUSTNESS_RULES; }
    public static List<QualityRule> getWebApiRules() { return WEB_API_RULES; }
    public static List<QualityRule> getMicroserviceRules() { return MICROSERVICE_RULES; }
    public static List<QualityRule> getTestQualityRules() { return TEST_QUALITY_RULES; }
    public static List<QualityRule> getInputValidationRules() { return INPUT_VALIDATION_RULES; }
    public static List<QualityRule> getCodeSmellEnhancedRules() { return CODE_SMELL_ENHANCED_RULES; }
    public static List<QualityRule> getSolidRules() { return SOLID_RULES; }
    public static List<QualityRule> getSecurityEnhancedRules() { return SECURITY_ENHANCED_RULES; }
    public static List<QualityRule> getResourceManagementRules() { return RESOURCE_MANAGEMENT_RULES; }
    public static List<QualityRule> getJava8PlusRules() { return JAVA8_PLUS_RULES; }
    public static List<QualityRule> getExceptionHandlingRules() { return EXCEPTION_HANDLING_RULES; }
    public static List<QualityRule> getLoggingRules() { return LOGGING_RULES; }
    public static List<QualityRule> getCollectionRules() { return COLLECTION_RULES; }
    public static List<QualityRule> getStringRules() { return STRING_RULES; }
    public static List<QualityRule> getDesignPatternRules() { return DESIGN_PATTERN_RULES; }
    public static List<QualityRule> getCodeOrganizationRules() { return CODE_ORGANIZATION_RULES; }
    public static List<QualityRule> getApiDesignRules() { return API_DESIGN_RULES; }
    public static List<QualityRule> getReflectionRules() { return REFLECTION_RULES; }
    public static List<QualityRule> getSecurityPerformanceRules() { return SECURITY_PERFORMANCE_RULES; }

    /**
     * Get an unmodifiable list of ALL rule categories combined.
     * Useful for diagnostics and testing.
     */
    public static List<List<QualityRule>> getAllRuleCategories() {
        return Collections.unmodifiableList(Arrays.asList(
            BUG_RULES, CODE_SMELL_RULES, SECURITY_RULES, OWASP_RULES,
            PERFORMANCE_RULES, ARCHITECTURE_RULES, MAINTAINABILITY_RULES,
            CONCURRENCY_RULES, MODERNIZATION_RULES, DATABASE_RULES,
            SPRING_BOOT_RULES, ROBUSTNESS_RULES, WEB_API_RULES,
            MICROSERVICE_RULES, TEST_QUALITY_RULES, INPUT_VALIDATION_RULES,
            CODE_SMELL_ENHANCED_RULES, SOLID_RULES, SECURITY_ENHANCED_RULES,
            RESOURCE_MANAGEMENT_RULES, JAVA8_PLUS_RULES, EXCEPTION_HANDLING_RULES,
            LOGGING_RULES, COLLECTION_RULES, STRING_RULES, DESIGN_PATTERN_RULES,
            CODE_ORGANIZATION_RULES, API_DESIGN_RULES, REFLECTION_RULES,
            SECURITY_PERFORMANCE_RULES
        ));
    }

    /**
     * Register ALL rules into the given RuleEngine, filtering by RulesConfig.
     * Replaces the 450+ lines of reg.accept() in SourceUniversePro.
     */
    public static void registerAll(RuleEngine engine, RulesConfig rulesConfig) {
        builder().withConfig(rulesConfig).registerTo(engine);
    }

    /**
     * Register ALL rules into the given RuleEngine, filtering by RulesConfig.
     * Legacy method - prefer using builder() for selective registration.
     */
    public static void registerAllLegacy(RuleEngine engine, RulesConfig rulesConfig) {
        registerCategory(engine, rulesConfig, BUG_RULES);
        registerCategory(engine, rulesConfig, CODE_SMELL_RULES);
        registerCategory(engine, rulesConfig, SECURITY_RULES);
        registerCategory(engine, rulesConfig, getCfgRules(rulesConfig));
        registerCategory(engine, rulesConfig, OWASP_RULES);
        registerCategory(engine, rulesConfig, PERFORMANCE_RULES);
        registerCategory(engine, rulesConfig, ARCHITECTURE_RULES);
        registerCategory(engine, rulesConfig, MAINTAINABILITY_RULES);
        registerCategory(engine, rulesConfig, CONCURRENCY_RULES);
        registerCategory(engine, rulesConfig, MODERNIZATION_RULES);
        registerCategory(engine, rulesConfig, DATABASE_RULES);
        registerCategory(engine, rulesConfig, SPRING_BOOT_RULES);
        registerCategory(engine, rulesConfig, ROBUSTNESS_RULES);
        registerCategory(engine, rulesConfig, WEB_API_RULES);
        registerCategory(engine, rulesConfig, MICROSERVICE_RULES);
        registerCategory(engine, rulesConfig, TEST_QUALITY_RULES);
        registerCategory(engine, rulesConfig, INPUT_VALIDATION_RULES);
        registerCategory(engine, rulesConfig, CODE_SMELL_ENHANCED_RULES);
        registerCategory(engine, rulesConfig, SOLID_RULES);
        registerCategory(engine, rulesConfig, SECURITY_ENHANCED_RULES);
        registerCategory(engine, rulesConfig, RESOURCE_MANAGEMENT_RULES);
        registerCategory(engine, rulesConfig, JAVA8_PLUS_RULES);
        registerCategory(engine, rulesConfig, EXCEPTION_HANDLING_RULES);
        registerCategory(engine, rulesConfig, LOGGING_RULES);
        registerCategory(engine, rulesConfig, COLLECTION_RULES);
        registerCategory(engine, rulesConfig, STRING_RULES);
        registerCategory(engine, rulesConfig, DESIGN_PATTERN_RULES);
        registerCategory(engine, rulesConfig, CODE_ORGANIZATION_RULES);
        registerCategory(engine, rulesConfig, API_DESIGN_RULES);
        registerCategory(engine, rulesConfig, REFLECTION_RULES);
        registerCategory(engine, rulesConfig, SECURITY_PERFORMANCE_RULES);
    }

    /**
     * Create a builder for selective rule registration.
     *
     * Example:
     *   RuleRegistry.builder()
     *       .withBugRules()
     *       .withSecurityRules()
     *       .excluding("RSPEC-108")
     *       .registerTo(engine);
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for selective rule registration.
     */
    public static class Builder {
        private final List<QualityRule> selectedRules = new ArrayList<>();
        private final Set<String> excludedKeys = new HashSet<>();
        private RulesConfig config;

        public Builder withBugRules() { selectedRules.addAll(BUG_RULES); return this; }
        public Builder withCodeSmellRules() { selectedRules.addAll(CODE_SMELL_RULES); return this; }
        public Builder withSecurityRules() { selectedRules.addAll(SECURITY_RULES); return this; }
        public Builder withOwaspRules() { selectedRules.addAll(OWASP_RULES); return this; }
        public Builder withPerformanceRules() { selectedRules.addAll(PERFORMANCE_RULES); return this; }
        public Builder withArchitectureRules() { selectedRules.addAll(ARCHITECTURE_RULES); return this; }
        public Builder withMaintainabilityRules() { selectedRules.addAll(MAINTAINABILITY_RULES); return this; }
        public Builder withConcurrencyRules() { selectedRules.addAll(CONCURRENCY_RULES); return this; }
        public Builder withModernizationRules() { selectedRules.addAll(MODERNIZATION_RULES); return this; }
        public Builder withDatabaseRules() { selectedRules.addAll(DATABASE_RULES); return this; }
        public Builder withSpringBootRules() { selectedRules.addAll(SPRING_BOOT_RULES); return this; }
        public Builder withRobustnessRules() { selectedRules.addAll(ROBUSTNESS_RULES); return this; }
        public Builder withWebApiRules() { selectedRules.addAll(WEB_API_RULES); return this; }
        public Builder withSecurityPerformanceRules() { selectedRules.addAll(SECURITY_PERFORMANCE_RULES); return this; }
        public Builder withConfig(RulesConfig config) { this.config = config; return this; }
        public Builder excluding(String... ruleKeys) {
            for (String key : ruleKeys) excludedKeys.add(key);
            return this;
        }

        /**
         * Register selected rules to the engine.
         */
        public void registerTo(RuleEngine engine) {
            RulesConfig cfg = config != null ? config : new RulesConfig();
            for (QualityRule rule : selectedRules) {
                if (cfg.isRuleEnabled(rule.getRuleKey()) && !excludedKeys.contains(rule.getRuleKey())) {
                    engine.registerRule(rule);
                }
            }
        }

        /**
         * Return the list of selected rules without registering.
         */
        public List<QualityRule> build() {
            return Collections.unmodifiableList(selectedRules);
        }
    }

    private static void registerCategory(RuleEngine engine, RulesConfig config, List<QualityRule> rules) {
        for (QualityRule rule : rules) {
            if (config.isRuleEnabled(rule.getRuleKey())) {
                engine.registerRule(rule);
            }
        }
    }

    /**
     * Get total count of all registered rules (for diagnostics).
     */
    public static int getTotalRuleCount() {
        return BUG_RULES.size() + CODE_SMELL_RULES.size() + SECURITY_RULES.size()
                + getCfgRules(new RulesConfig()).size() + OWASP_RULES.size()
                + PERFORMANCE_RULES.size() + ARCHITECTURE_RULES.size()
                + MAINTAINABILITY_RULES.size() + CONCURRENCY_RULES.size()
                + MODERNIZATION_RULES.size() + DATABASE_RULES.size()
                + SPRING_BOOT_RULES.size() + ROBUSTNESS_RULES.size()
                + WEB_API_RULES.size() + MICROSERVICE_RULES.size()
                + TEST_QUALITY_RULES.size() + INPUT_VALIDATION_RULES.size()
                + CODE_SMELL_ENHANCED_RULES.size() + SOLID_RULES.size()
                + SECURITY_ENHANCED_RULES.size() + RESOURCE_MANAGEMENT_RULES.size()
                + JAVA8_PLUS_RULES.size() + EXCEPTION_HANDLING_RULES.size()
                + LOGGING_RULES.size() + COLLECTION_RULES.size()
                + STRING_RULES.size() + DESIGN_PATTERN_RULES.size()
                + CODE_ORGANIZATION_RULES.size() + API_DESIGN_RULES.size()
                + REFLECTION_RULES.size() + SECURITY_PERFORMANCE_RULES.size();
    }
}
