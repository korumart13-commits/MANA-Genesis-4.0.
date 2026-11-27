package com.mana.brain

import java.nio.file.Files
import java.nio.file.Path

/** Command-line entry point used by the disposable GitHub Actions patch-validation job. */
fun main(args: Array<String>) {
    require(args.size == 1) { "Usage: EngineeringPatchPolicyCli <patch-file>" }
    val patchFile = Path.of(args.single()).toAbsolutePath().normalize()
    require(Files.isRegularFile(patchFile)) { "Patch file does not exist." }
    val diff = Files.readString(patchFile)
    val validation = EngineeringPatchPolicy.validate(diff)
    println("MANA_PATCH_VALIDATION=PASS")
    println("changed_files=${validation.changedPaths.size}")
    println("additions=${validation.additions}")
    println("deletions=${validation.deletions}")
    validation.changedPaths.sorted().forEach { println("path=$it") }
}
