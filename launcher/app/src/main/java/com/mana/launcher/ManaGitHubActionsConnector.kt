package com.mana.launcher

import android.content.Context
import com.mana.brain.GitHubActionsRestClient
import com.mana.brain.GitHubTokenProvider
import com.mana.brain.ManaEngineeringAutomation
import com.mana.brain.ManaEngineeringCopilot
import com.mana.brain.EngineeringTask
import com.mana.brain.EngineeringAutomationResult
import com.mana.brain.InferenceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Android bridge to GitHub Actions; credentials are encrypted by Android Keystore. */
class ManaGitHubActionsConnector(context: Context) {
    private val credentials = ManaEncryptedCredentialStore(
        context.applicationContext, "github-actions.enc", "mana.github.actions.v1"
    )

    fun isConfigured(): Boolean = runCatching { !credentials.read().isNullOrBlank() }.getOrDefault(false)

    fun configure(token: String) {
        val value = token.trim()
        require(value.length in 20..512) { "Проверь GitHub token: длина выглядит некорректной." }
        require(value.startsWith("github_pat_") || value.startsWith("ghp_") || value.startsWith("ghs_")) {
            "Используй fine-grained PAT или installation token GitHub App."
        }
        credentials.write(value)
    }

    fun removeCredentials() = credentials.clear()

    suspend fun run(
        provider: InferenceProvider,
        task: EngineeringTask,
        branch: String = DEFAULT_BRANCH,
        maxRepairAttempts: Int = 2
    ): EngineeringAutomationResult = withContext(Dispatchers.IO) {
        require(isConfigured()) { "GitHub Actions не подключён: сначала сохрани токен." }
        require(provider.isAvailable()) { "Инженерный облачный провайдер MANA не настроен или выключен." }
        val gateway = GitHubActionsRestClient(
            repository = REPOSITORY,
            workflowFile = WORKFLOW_FILE,
            tokenProvider = GitHubTokenProvider { credentials.read() }
        )
        val headSha = gateway.currentHeadSha(branch)
        val automation = ManaEngineeringAutomation(
            copilot = ManaEngineeringCopilot(provider),
            github = gateway,
            branch = branch,
            maxRepairAttempts = maxRepairAttempts
        )
        automation.run(task, headSha)
    }

    companion object {
        const val REPOSITORY = "korumart13-commits/MANA-Genesis-4.0."
        const val DEFAULT_BRANCH = "chatgpt/mana-asi-rmx5566-one-action-setup"
        const val WORKFLOW_FILE = "mana-engineering-pipeline.yml"
    }
}
