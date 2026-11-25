# MANA Smart Receptionist — Android telephony capability boundary

Android 16 does not expose one API that both screens a call and automatically answers it.

## Call screening

MANA can request the Android call-screening role and implement CallScreeningService. The system binds the user-selected screening app before the phone rings. The service must respond within five seconds, and its response can allow, silence, or block the call. It cannot wait ten seconds and then answer the call.

Official API: https://developer.android.com/reference/android/telecom/CallScreeningService

## Automatic answering

To answer and manage a live call programmatically, an app must be the user's default phone/dialer app and implement InCallService plus the required dialer UI and ACTION_DIAL entry points. This is a much larger role with additional user-facing responsibilities. The platform always uses the preloaded dialer for emergency calls.

Official API: https://developer.android.com/reference/android/telecom/InCallService

## MANA implementation policy

- Keep call screening and call answering as separate capabilities.
- The initial MANA build may identify callers, consult local contacts, apply an owner-approved allow/block/silence policy, and show a caller card.
- Never claim the existing CallScreeningService implements auto-answer.
- The requested 10-second auto-answer delay can only be implemented in a fully compliant default-dialer/InCallService design, after the owner explicitly chooses MANA as the default phone app and the incoming-call UI, ongoing-call UI, emergency-call fallback and accessibility/privacy behavior are complete.
- If the owner does not grant the relevant role, the system must leave the stock dialer in control.
- Do not delay the mandatory CallScreeningService response while running AI inference. Make a bounded local decision and respond within the platform deadline; perform summaries only after the call flow permits it.
