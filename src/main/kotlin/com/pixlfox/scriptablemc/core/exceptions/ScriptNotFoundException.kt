package com.pixlfox.scriptablemc.core.exceptions

import java.io.File

class ScriptNotFoundException(scriptFile: File) : Exception("Unable to load script: ${scriptFile.path}.")