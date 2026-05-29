export interface LlmSettings {
  provider: string
  baseUrl: string
  model: string
  apiKeyConfigured: boolean
  apiKeyMasked: string
}

export interface LlmSettingsForm {
  provider: string
  baseUrl: string
  model: string
  apiKey: string
}

export type SettingsTab = 'llm' | 'database'
