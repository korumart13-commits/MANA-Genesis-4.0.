import unittest
from pathlib import Path


class ManaAvatarVoiceUiTests(unittest.TestCase):
    def setUp(self):
        self.source = Path(
            "launcher/app/src/main/java/com/mana/launcher/MainActivity.kt"
        ).read_text(encoding="utf-8")

    def test_avatar_is_the_only_visible_voice_entry_point_in_assistant_panel(self):
        self.assertIn("top.addView(createAvatarSceneView { startVoiceInput() }", self.source)
        self.assertIn('contentDescription = "3D-аватар MANA. Коснись, чтобы поговорить."', self.source)
        self.assertNotIn('action("Голос")', self.source)

    def test_no_external_google_or_system_recognition_activity_is_launched(self):
        self.assertNotIn("startActivityForResult(voiceIntent()", self.source)
        self.assertNotIn("startSystemVoiceInput", self.source)
        self.assertIn("SpeechRecognizer.createSpeechRecognizer(this)", self.source)
        self.assertIn("SpeechRecognizer.createOnDeviceSpeechRecognizer(this)", self.source)

    def test_speech_results_are_returned_to_mana_and_spoken_by_avatar(self):
        self.assertIn("chatInput?.setText(spoken)", self.source)
        self.assertIn("sendToLocalModel()", self.source)
        self.assertIn("ManaAvatarState.expressionIndex = 1", self.source)
        self.assertIn("speakResponse", self.source)


if __name__ == "__main__":
    unittest.main()
