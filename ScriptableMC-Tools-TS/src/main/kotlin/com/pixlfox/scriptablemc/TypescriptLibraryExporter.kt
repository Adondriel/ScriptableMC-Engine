package com.pixlfox.scriptablemc

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.thoughtworks.paranamer.BytecodeReadingParanamer
import com.thoughtworks.paranamer.Paranamer
import org.bukkit.plugin.PluginDescriptionFile
import java.io.File
import java.lang.reflect.*


@Suppress("MemberVisibilityCanBePrivate", "UnstableApiUsage", "unused")
class TypescriptLibraryExporter(args: Array<String> = arrayOf()) {
    private var basePath: String = "./lib"
    private val classList = mutableListOf<Class<*>>()
    private var allowedPackagesRegex: Regex = Regex("(org\\.bukkit|com\\.pixlfox|com\\.smc|fr\\.minuskube\\.inv|com\\.google|java\\.sql|java\\.io|java\\.nio|khttp|org\\.apache\\.commons\\.io)(.*)?")
    private val paranamer: Paranamer = BytecodeReadingParanamer()
    private val isRelease: Boolean = args.contains("--release")

    private fun safeName(name: String): String = when {
        name.equals("function", true) -> "_function"
        name.equals("yield", true) -> "_yield"
        name.equals("arguments", true) -> "_arguments"
        name.equals("name", true) -> "_name"
        name.equals("<set-?>", true) -> "value"
        name.equals("in", true) -> "_in"
        else -> name
    }

    private fun safeClassName(name: String): String = when {
        name.equals("Array", false) -> "_Array"
        else -> name
    }

    private fun javaClassToTypescript(_class: Class<*>?, genericType: Type? = null): String {
        if(_class == null) {
            return "any"
        }

        val className = stripPackageName(_class.name)
        val packageName = getPackageName(_class.name)

        if(_class.isArray) {
            return "Array<${javaClassToTypescript(_class.componentType)}>"
        }

        if(className.equals("List", false)) {
            if(genericType != null && genericType is ParameterizedType) {
                val actualTypeArg = genericType.actualTypeArguments.firstOrNull()
                if(actualTypeArg != null && actualTypeArg is Class<*>) {
                    return "Array<${javaClassToTypescript(actualTypeArg)}>"
                }
            }

            return "Array<any>"
        }

        return if (className.equals("byte", true) || className.equals("short", true) || className.equals("int", true) || className.equals("long", true)) "number"
        else if (className.equals("double", true) || className.equals("float", true)) "number"
        else if (className.equals("string", true) || className.equals("char", true) || className.equals("UUID", true)) "string"
        else if (className.equals("void", true)) "void"
        else if (className.equals("boolean", true)) "boolean"
        else if (className.equals("Value", true)) "any"
        else if (!packageName.matches(allowedPackagesRegex)) {
            //println(_class.name)
            "any"
        }
        else className
    }

    fun allowPackages(regex: String): TypescriptLibraryExporter {
        this.allowedPackagesRegex = Regex(regex)
        return this
    }

    fun basePath(basePath: String): TypescriptLibraryExporter {
        this.basePath = basePath
        return this
    }

    fun clean(skipThisDir: Boolean = true): TypescriptLibraryExporter {
        deleteDirectory(File(basePath), skipThisDir)
        return this
    }

    fun addHelperClasses(): TypescriptLibraryExporter {
        addClasses(
            com.pixlfox.scriptablemc.core.ScriptablePluginContext::class.java,
            com.pixlfox.scriptablemc.core.ScriptablePluginEngine::class.java,
            ScriptEngineMain::class.java,

            com.smc.utils.ItemBuilder::class.java,
            com.smc.utils.MysqlWrapper::class.java,
            com.smc.utils.Http::class.java,

            com.smc.version.Version::class.java,
            com.smc.version.MinecraftVersions::class.java,

            fr.minuskube.inv.SmartInventory::class.java,
            com.smc.smartinvs.SmartInventoryProvider::class.java,
            com.smc.smartinvs.SmartInventory::class.java,

            org.apache.commons.io.FileUtils::class.java,

            com.google.common.io.ByteStreams::class.java,

            khttp.structures.files.FileLike::class.java,
            khttp.structures.authorization.Authorization::class.java,
            khttp.structures.authorization.BasicAuthorization::class.java,
            khttp.structures.cookie.Cookie::class.java,
            khttp.structures.cookie.CookieJar::class.java,
            khttp.structures.maps.CaseInsensitiveMap::class.java,
            khttp.structures.parameters.Parameters::class.java,
            khttp.requests.GenericRequest::class.java,
            khttp.requests.Request::class.java,
            khttp.responses.GenericResponse::class.java,
            khttp.responses.Response::class.java,

            java.io.File::class.java
        )
        return this
    }

    fun addBukkitClasses(): TypescriptLibraryExporter {
        val classLoader = javaClass.classLoader
        val bukkitTypes = Parser.default().parse("./type-search-index.json")

        if(bukkitTypes is JsonArray<*>) {
            for (bukkitType in bukkitTypes) {
                if(bukkitType is JsonObject) {
                    val packageName = bukkitType.string("p")
                    val className = bukkitType.string("l")

                    try {
                        this.addClass(classLoader.loadClass("$packageName.$className"))
                    } catch (e: ClassNotFoundException) { }
                }
            }
        }

        return this
    }

    fun addClasses(vararg _classes: Class<*>): TypescriptLibraryExporter {
        for (_class in _classes) {
            addClass(_class)
        }

        return this
    }

    fun addClass(_class: Class<*>): TypescriptLibraryExporter {
        if (!_class.isArray) {
            if (!classList.contains(_class) && _class.name.matches(allowedPackagesRegex)) {
                classList.add(_class)
            } else {
                return this
            }

            for (_method in _class.methods) {
                val returnType = fixClass(_method.returnType)
                if (returnType.name.matches(allowedPackagesRegex)) {
                    addClass(returnType)
                }

                for (_parameter in _method.parameters) {
                    val type = fixClass(_parameter.type)
                    if (type.name.matches(allowedPackagesRegex)) {
                        addClass(type)
                    }
                }
            }

            for (_field in _class.fields) {
                val fieldType = fixClass(_field.type)
                if (!fieldType.name.endsWith("\$Companion") && fieldType.name.matches(allowedPackagesRegex)) {
                    addClass(fieldType)
                }
            }

            for (_constructor in _class.constructors) {
                for (_parameter in _constructor.parameters) {
                    val type = fixClass(_parameter.type)
                    if (type.name.matches(allowedPackagesRegex)) {
                        addClass(type)
                    }
                }
            }


            for (_interface in _class.interfaces) {
                if (!classList.contains(_interface)) {
                    if (_interface.name.matches(allowedPackagesRegex)) {
                        addClass(_interface)
                    }
                }
            }

            if (_class.superclass != null && _class.superclass.name.matches(allowedPackagesRegex)) {
                if(!classList.contains(_class.superclass)) {
                    addClass(_class.superclass)
                }
            }
        }

        return this
    }

    private fun buildClassList(_class: Class<*>): Array<Class<*>> {
        val classList = mutableListOf<Class<*>>()

        for (_method in _class.methods) {
            val returnType = fixClass(_method.returnType)
            if (!classList.contains(returnType)) {
                classList.add(returnType)
            }

            val genericType = _method.genericReturnType
            if(stripPackageName(returnType.name).equals("List", false)) {
                if(genericType != null && genericType is ParameterizedType) {
                    val actualTypeArg = genericType.actualTypeArguments.firstOrNull()
                    if(actualTypeArg != null && actualTypeArg is Class<*>) {
                        if (!classList.contains(actualTypeArg)) {
                            classList.add(actualTypeArg)
                        }
                    }
                }
            }

            for (_parameter in _method.parameters) {
                val type = fixClass(_parameter.type)
                if (!classList.contains(type)) {
                    classList.add(type)
                }
            }
        }

        for (_constructor in _class.constructors) {
            for (_parameter in _constructor.parameters) {
                val type = fixClass(_parameter.type)
                if (!classList.contains(type) && type.name.matches(allowedPackagesRegex)) {
                    classList.add(type)
                }
            }
        }

        for (_interface in _class.interfaces) {
            if (!classList.contains(_interface)) {
                if (_interface.name.matches(allowedPackagesRegex)) {
                    classList.add(_interface)
                }
            }
        }

        if (_class.superclass != null && _class.superclass.name.matches(allowedPackagesRegex)) {
            if (!classList.contains(_class.superclass)) {
                classList.add(_class.superclass)
            }
        }

        val blacklistRegex = Regex("(spigot|wait|equals|toString|hashCode|getClass|notify|notifyAll|Companion)")
        for (_field in _class.fields.filter { Modifier.isStatic(it.modifiers) &&Modifier.isPublic(it.modifiers) && !it.name.matches(blacklistRegex) }) {
                val type = fixClass(_field.type)
                if (!classList.contains(type) && type.name.matches(allowedPackagesRegex)) {
                    classList.add(type)
                }
        }

        return classList.toTypedArray()
    }

    private fun buildClassList(_classes: Array<out Class<*>>): Array<Class<*>> {
        val classList = mutableListOf<Class<*>>()

        for(_class in _classes) {
            for (_method in _class.methods) {
                val returnType = fixClass(_method.returnType)
                if (!classList.contains(returnType)) {
                    classList.add(returnType)
                }

                for (_parameter in _method.parameters) {
                    val type = fixClass(_parameter.type)
                    if (!classList.contains(type)) {
                        classList.add(type)
                    }
                }
            }

            for (_constructor in _class.constructors) {
                for (_parameter in _constructor.parameters) {
                    val type = fixClass(_parameter.type)
                    if (!classList.contains(type) && type.name.matches(allowedPackagesRegex)) {
                        classList.add(type)
                    }
                }
            }

            for (_interface in _class.interfaces) {
                if (!classList.contains(_interface)) {
                    if (_interface.name.matches(allowedPackagesRegex)) {
                        classList.add(_interface)
                    }
                }
            }

            if (_class.superclass != null && _class.superclass.name.matches(allowedPackagesRegex)) {
                if (!classList.contains(_class.superclass)) {
                    classList.add(_class.superclass)
                }
            }
        }

        return classList.toTypedArray()
    }

    fun exportLibraries(): TypescriptLibraryExporter {
        var count = 0

        for ((packageName, classes) in classList.sortedBy { stripPackageName(it.name) }.groupBy { getPackageName(it.name) }) {
            for (_class in classes) {
                if(_class.name.matches(allowedPackagesRegex) && !_class.name.endsWith("\$Spigot")) {
                    count++

                    val file = File("$basePath/ts/${getPackageName(_class.name).replace('.', '/')}/${stripPackageName(_class.name)}.ts")
                    if(!file.exists()) {
                        file.parentFile.mkdirs()
                        file.createNewFile()
                        file.writeText(generateTypescriptSource(_class))
                        println("Exported $packageName.${stripPackageName(_class.name)} -> ${file.path}.")
                    }
                }
            }
        }

        println("Successfully generated $count class libraries.")

        return this
    }

    fun exportGlobalLibrary(): TypescriptLibraryExporter {
        val file = File("$basePath/ts/global.ts")
        if(!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
            file.writeText(generateTypescriptGlobalExports())
            println("Exported global.ts.")
        }

        return this
    }

    fun exportIndexLibrary(): TypescriptLibraryExporter {
        val file = File("$basePath/ts/index.ts")
        if(!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
            file.writeText(generateTypescriptGlobalExports())
            println("Exported index.ts.")
        }

        return this
    }

    fun copyStaticSources(): TypescriptLibraryExporter {
        File("./src/main/ts/").copyRecursively(File("$basePath/ts/"), true)

        return this
    }

    fun exportProjectFiles(): TypescriptLibraryExporter {
        File("$basePath/tsconfig.json").writeText("{\n" +
                "    \"compilerOptions\": {\n" +
                "        \"target\": \"es2019\",\n" +
                "        \"module\": \"esnext\",\n" +
                "        \"sourceMap\": false,\n" +
                "        \"allowJs\": true,\n" +
                "        \"outDir\": \"js\",\n" +
                "        \"rootDir\": \"ts\",\n" +
                "        \"declaration\": true\n" +
                "    },\n" +
                "    \"include\": [\n" +
                "        \"ts/**/*\"\n" +
                "    ]\n" +
                "}")

        File("$basePath/package.json").writeText("{\n" +
                "  \"name\": \"scriptablemc-typescript-lib\",\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"description\": \"Typescript plugin example and libraries for Minecraft 1.15\",\n" +
                "  \"scripts\": {\n" +
                "    \"compile\": \"npx tsc\",\n" +
                "    \"compile_commonjs\": \"npx tsc -m commonjs --outDir ./commonjs\"\n" +
                "  },\n" +
                "  \"author\": \"Ashton Storks\",\n" +
                "  \"license\": \"ISC\",\n" +
                "  \"bugs\": {\n" +
                "    \"url\": \"https://github.com/astorks/ScriptableMC-TypeScript/issues\"\n" +
                "  },\n" +
                "  \"homepage\": \"https://github.com/astorks/ScriptableMC-TypeScript#readme\",\n" +
                "  \"dependencies\": {},\n" +
                "  \"devDependencies\": {\n" +
                "    \"typescript\": \"^3.7.5\"\n" +
                "  }\n" +
                "}\n")

        return this
    }

    fun exportCommonJSProjectFiles(): TypescriptLibraryExporter {
        File("$basePath/tsconfig.json").writeText("{\n" +
                "    \"compilerOptions\": {\n" +
                "        \"target\": \"es2019\",\n" +
                "        \"module\": \"commonjs\",\n" +
                "        \"sourceMap\": false,\n" +
                "        \"allowJs\": true,\n" +
                "        \"outDir\": \"js\",\n" +
                "        \"rootDir\": \"ts\",\n" +
                "        \"declaration\": true\n" +
                "    },\n" +
                "    \"include\": [\n" +
                "        \"ts/**/*\"\n" +
                "    ]\n" +
                "}")

        val pluginDescription = PluginDescriptionFile(File("../ScriptableMC-Engine-JS/src/main/resources/plugin.yml").inputStream())
        val githubSha = System.getenv().getOrElse("GITHUB_SHA") { "" }
        val githubTag = System.getenv().getOrElse("GITHUB_REF") { "" }

        val isReleaseTag = githubTag.startsWith("refs/tags/v") && isRelease

        val version = when {
            isReleaseTag -> githubTag.substring(11)
            isRelease && githubSha.isNullOrEmpty() -> pluginDescription.version
            else -> "${pluginDescription.version}-dev-$githubSha"
        }

        if(isRelease) {
            File("$basePath/.npmrc").writeText("//registry.npmjs.org/:_authToken=\${NPM_TOKEN}")
        }
        else {
            File("$basePath/.npmrc").writeText("//npm.pkg.github.com/:_authToken=\${GITHUB_TOKEN}\n" +
                    "registry=https://npm.pkg.github.com/astorks")
        }

        File("$basePath/package.json").writeText("{\n" +
                "  \"name\": \"@astorks/lib-smc\",\n" +
                "  \"repository\": \"git@github.com:astorks/ScriptableMC-Engine.git\",\n" +
                "  \"version\": \"$version\",\n" +
                "  \"description\": \"JavaScript CommonJS libraries for ScriptableMC\",\n" +
                "  \"scripts\": {\n" +
                "    \"compile\": \"npx tsc\",\n" +
                "    \"postcompile\": \"cp ./package.json ./js/package.json && cp ./.npmrc ./js/.npmrc\"\n" +
                "  },\n" +
                "  \"author\": \"Ashton Storks\",\n" +
                "  \"license\": \"ISC\",\n" +
                "  \"bugs\": {\n" +
                "    \"url\": \"https://github.com/astorks/ScriptableMC-TypeScript/issues\"\n" +
                "  },\n" +
                "  \"homepage\": \"https://github.com/astorks/ScriptableMC-TypeScript#readme\",\n" +
                "  \"dependencies\": {},\n" +
                "  \"devDependencies\": {\n" +
                "    \"typescript\": \"^3.7.5\"\n" +
                "  }\n" +
                "}")

        return this
    }

    @Suppress("UNCHECKED_CAST")
    private fun generateTypescriptSource(_class: Class<*>): String {
        var source = "declare var Java: any;\n"
        source += generateTypescriptImports(_class)
        source += generateTypescriptInterface(_class)

        source += if(_class.isEnum) generateTypescriptEnum(_class as Class<Enum<*>>) else generateTypescriptClass(_class)

        return source
    }


    private fun generateTypescriptImports(_class: Class<*>): String {
        var tsImportsSource = ""

        val classList = buildClassList(_class)
        for(requiredClass in classList.sortedBy { safeClassName(stripPackageName(it.name)) }) {
            val packageName = getPackageName(requiredClass.name)

            if(!stripPackageName(_class.name).equals(stripPackageName(requiredClass.name), true)) {
                if (packageName.matches(allowedPackagesRegex) && !requiredClass.name.endsWith("\$Spigot")) {
                    val upDirCount = getPackageName(_class.name).split('.').count()

                    tsImportsSource += "import ${safeClassName(stripPackageName(requiredClass.name))} from '${"../".repeat(upDirCount)}${getPackageName(requiredClass.name).replace('.', '/')}/${stripPackageName(requiredClass.name)}.js'\n"
                }
            }
        }

        return tsImportsSource + "\n"
    }

    private fun generateTypescriptImportForClass(_class: Class<*>): String {
        return "import ${getPackageName(_class.name).replace('.', '_')}_${safeClassName(stripPackageName(_class.name))} from './${getPackageName(_class.name).replace('.', '/')}/${stripPackageName(_class.name)}.js'\n"
    }

    private fun generateTypescriptGlobalExports(): String {
        var tsGlobalExportsSource = ""

        for (_class in classList.sortedBy { safeClassName(stripPackageName(it.name)) }) {
            if(_class.name.matches(allowedPackagesRegex) && !_class.name.endsWith("\$Spigot")) {
                tsGlobalExportsSource += generateTypescriptImportForClass(_class)
            }
        }

        for((packageName, classes) in this.classList.sortedBy { safeClassName(stripPackageName(it.name)) }.groupBy { getPackageName(it.name) }) {
            tsGlobalExportsSource += "export namespace $packageName {\n"
            for (_class in classes) {
                if(_class.name.matches(allowedPackagesRegex) && !_class.name.endsWith("\$Spigot")) {
                    tsGlobalExportsSource += "\texport const ${safeClassName(stripPackageName(_class.name))} = ${getPackageName(_class.name).replace('.', '_')}_${safeClassName(stripPackageName(_class.name))};\n"
                }
            }
            tsGlobalExportsSource += "}\n"
        }

        return tsGlobalExportsSource + "\n"
    }

    private fun generateTypescriptInterface(_class: Class<*>): String {
        var methodCount = 0
        var tsInterfaceSource = "export default interface ${safeClassName(stripPackageName(_class.name))}"

        val interfaceNames = getInterfaceNames(_class)

        if(interfaceNames.any()) {
            tsInterfaceSource += " extends ${interfaceNames.joinToString(", ")}"
        }

        tsInterfaceSource += " {\n"

        val blacklistRegex = Regex("(spigot|wait|equals|toString|hashCode|getClass|notify|notifyAll|(.*?)\\\$(.*?))")
        for (_method in _class.methods.sortedWith(compareBy({it.name}, {it.parameterCount}))) {
            if(!Modifier.isStatic(_method.modifiers) && Modifier.isPublic(_method.modifiers) && !_method.name.matches(blacklistRegex)) {
                methodCount++
                tsInterfaceSource += if (_method.parameterCount == 0) {
                    "\t${_method.name}(): ${javaClassToTypescript(_method.returnType, _method.genericReturnType)};\n"
                } else {
                    "\t${_method.name}(${getParameters(_method).joinToString(", ")}): ${safeClassName(javaClassToTypescript(_method.returnType, _method.genericReturnType))};\n"
                }
            }
        }

        tsInterfaceSource += "}\n\n"

        if(methodCount == 0) {
            return ""
        }

        return tsInterfaceSource
    }

    private fun generateTypescriptClass(_class: Class<*>): String {
        val className = safeClassName(stripPackageName(_class.name))
        var tsClassSource = "export default class $className {\n"

        tsClassSource += "\tpublic static get \$javaClass(): any {\n"
        tsClassSource += "\t\treturn Java.type('${_class.name}');\n"
        tsClassSource += "\t}\n\n"

        for ((index, constructor) in _class.constructors.sortedBy { it.parameterCount }.withIndex()) {
            tsClassSource += "\tconstructor(${getParameters(constructor).joinToString(", ")});\n"
            if(index == _class.constructors.size - 1) {
                tsClassSource += "\tconstructor(...args: any[]) {\n"
                tsClassSource += "\t\treturn new $className.\$javaClass(...args);\n"
                tsClassSource += "\t}\n\n"
            }
        }

        val countMap = mutableMapOf<String, Int>()
        val blacklistRegex = Regex("(spigot|wait|equals|toString|hashCode|getClass|notify|notifyAll|Companion)")
        for (_field in _class.fields.sortedBy { it.name }) {
            var jsFieldName: String = _field.name

            if(!countMap.containsKey(_field.name)) {
                countMap[_field.name] = 0
            }
            else {
                jsFieldName = "$jsFieldName${countMap[_field.name]}"
                countMap[_field.name] = (countMap[_field.name]!! + 1)
            }

            if(Modifier.isStatic(_field.modifiers) && Modifier.isPublic(_field.modifiers) && Modifier.isFinal(_field.modifiers) && !_field.name.matches(blacklistRegex)) {
                tsClassSource += "\tpublic static get ${safeName(jsFieldName)}(): ${safeClassName(javaClassToTypescript(_field.type))} {\n"
                tsClassSource += "\t\treturn $className.\$javaClass.${_field.name};\n"
                tsClassSource += "\t}\n\n"
            }
        }

        for (_methodGroups in _class.methods.sortedWith(compareBy({it.name}, {it.parameterCount})).filter { e -> Modifier.isStatic(e.modifiers) && !Modifier.isPrivate(e.modifiers) }.groupBy { e -> e.name }) {

            val jsMethodName: String = _methodGroups.key

            for ((index, _method) in _methodGroups.value.withIndex()) {
                if(!countMap.containsKey(_method.name)) {
                    countMap[_method.name] = 0
                }
                else {
                    //jsMethodName = "$jsMethodName${countMap[_method.name]}"
                    countMap[_method.name] = (countMap[_method.name]!! + 1)
                }

                if(Modifier.isStatic(_method.modifiers) && Modifier.isPublic(_method.modifiers) && !_method.name.matches(blacklistRegex)) {
                    tsClassSource += "\tpublic static ${safeName(jsMethodName)}(${getParameters(_method).joinToString(", ")}): ${javaClassToTypescript(_method.returnType, _method.genericReturnType)};\n"
                    if(index == _methodGroups.value.size - 1) {
                        tsClassSource += "\tpublic static ${safeName(jsMethodName)}(...args: any[]): any {\n"
                        tsClassSource += "\t\treturn $className.\$javaClass.${_method.name}(...args);\n"
                        tsClassSource += "\t}\n\n"
                    }
                }
            }
        }

        tsClassSource += "}\n\n"

        return tsClassSource
    }

    private fun generateTypescriptEnum(_class: Class<Enum<*>>): String {
        val enumName = safeClassName(stripPackageName(_class.name))
        var tsEnumSource = "export default class $enumName {\n"

        tsEnumSource += "\tpublic static get \$javaClass(): any {\n"
        tsEnumSource += "\t\treturn Java.type('${_class.name}');\n"
        tsEnumSource += "\t}\n\n"

        for (enumConstant in _class.enumConstants.sortedBy { it.name }) {
            tsEnumSource += "\tpublic static get ${enumConstant.name}(): $enumName {\n"
            tsEnumSource += "\t\treturn this.\$javaClass.${enumConstant.name};\n"
            tsEnumSource += "\t}\n"
        }

        tsEnumSource += "}\n\n"

        return tsEnumSource
    }

    private fun getInterfaceNames(_class: Class<*>): Array<String> {
        val interfaceNames = mutableListOf<String>()

        if(_class.superclass != null && _class.superclass.name.matches(allowedPackagesRegex)) {
            interfaceNames.add(stripPackageName(_class.superclass.name))
        }

        for(_interface in _class.interfaces) {
            if(_interface.name.startsWith("org.bukkit")) {
                interfaceNames.add(stripPackageName(_interface.name))
            }
        }

        return interfaceNames.toTypedArray()
    }

    private fun getParameters(_parameters: Array<Parameter>): Array<String> {
        val parameterNames = mutableListOf<String>()

        for(_parameter in _parameters) {
            parameterNames.add("${safeName(_parameter.name)}: ${safeClassName(javaClassToTypescript(_parameter.type))}")
        }

        return parameterNames.toTypedArray()
    }

    private fun getParameters(_method: Method): Array<String> {
        val parameterNames = mutableListOf<String>()
        val paranames = paranamer.lookupParameterNames(_method, false)

        for((index, _parameter) in _method.parameters.withIndex()) {
            parameterNames.add("${safeName(paranames.getOrElse(index) { _parameter.name })}: ${safeClassName(javaClassToTypescript(_parameter.type))}")
        }

        return parameterNames.toTypedArray()
    }

    private fun getParameters(_constructor: Constructor<*>): Array<String> {
        val parameterNames = mutableListOf<String>()
        val parameterNamesLookup = paranamer.lookupParameterNames(_constructor, false)

        for((index, _parameter) in _constructor.parameters.withIndex()) {
            parameterNames.add("${safeName(parameterNamesLookup.getOrElse(index) { _parameter.name })}: ${safeClassName(javaClassToTypescript(_parameter.type))}")
        }

        return parameterNames.toTypedArray()
    }

    private fun getParameterNames(_parameters: Array<Parameter>): Array<String> {
        val parameterNames = mutableListOf<String>()

        for(_parameter in _parameters) {
            parameterNames.add(safeName(_parameter.name))
        }

        return parameterNames.toTypedArray()
    }

    private fun stripPackageName(name: String): String {
        return name.split('.').last().replace(";", "")
    }

    private fun getPackageName(name: String): String {
        val packages = name.split('.').toMutableList()

        if(packages.count() > 1) {
            packages.remove(packages.last())
        }

        return packages.joinToString(".")
    }

    private fun fixClass(_class: Class<*>): Class<*> {
        if(_class.isArray) {
            return fixClass(_class.componentType)
        }

        return _class
    }

    private fun deleteDirectory(directoryToBeDeleted: File, skipThisDir: Boolean = false): Boolean {
        val allContents = directoryToBeDeleted.listFiles()
        if (allContents != null) {
            for (file in allContents) {
                deleteDirectory(file)
            }
        }

        return if(skipThisDir) {
            true
        }
        else {
            directoryToBeDeleted.delete()
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if(args.contains("--lib-smc")) {
                TypescriptLibraryExporter(args)
                    .basePath("./lib-smc")
                    .addHelperClasses()
                    .addBukkitClasses()
                    .clean()
                    .exportLibraries()
                    .exportIndexLibrary()
                    .copyStaticSources()
                    .exportCommonJSProjectFiles()
            }
            else {
                TypescriptLibraryExporter(args)
                    .addHelperClasses()
                    .addBukkitClasses()
                    .clean()
                    .exportLibraries()
                    .exportGlobalLibrary()
                    .copyStaticSources()
                    .exportProjectFiles()
            }
        }
    }
}