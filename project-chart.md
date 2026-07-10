# Project Charts — App Lock

## 1. App Launch Intercept Flow (Critical Path)

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ User Clicks │───▶│ Accessibility│───▶│  Is Locked? │──▶│   YES →     │
│  App Icon   │    │  Detects    │    │   Check DB  │    │ Block Launch│
└─────────────┘    │  Window     │    └──────┬──────┘    └──────┬──────┘
                   │  Change     │           │ NO                │
                   └─────────────┘           ▼                   ▼
                                      ┌─────────────┐    ┌─────────────┐
                                      │ Allow Normal│    │ performGlobal│
                                      │    Launch   │    │ Action(HOME) │
                                      └─────────────┘    └──────┬──────┘
                                                                │
                                                                ▼
                                                         ┌─────────────┐
                                                         │ Start       │
                                                         │ LockScreen  │
                                                         │ Activity    │
                                                         └──────┬──────┘
                                                                │
                                                                ▼
                    ┌──────────────────────────────────────────────────────┐
                    │              LOCK SCREEN ACTIVITY                      │
                    │  ┌─────────────┐    ┌─────────────┐    ┌──────────┐ │
                    │  │ Show PIN/   │───▶│  Validate   │───▶│ CORRECT? │ │
                    │  │ Pattern UI  │    │  Input      │    └────┬─────┘ │
                    │  └─────────────┘    └─────────────┘         │      │
                    │                                           │ YES  │ NO
                    │                                           ▼      ▼
                    │                                    ┌────────┐ ┌────────┐
                    │                                    │ Launch │ │ Shake  │
                    │                                    │ Target │ │ + Error│
                    │                                    │  App   │ │ + Retry│
                    │                                    └───┬────┘ └───┬────┘
                    │                                        │            │
                    └────────────────────────────────────────┘            │
                                                                          │
                    ┌─────────────────────────────────────────────────────┘
                    │
                    ▼
              ┌─────────────┐
              │ finishAnd   │
              │ RemoveTask()│  ← LockScreen gone from recents
              └─────────────┘
```

---

## 2. Data Flow Architecture

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Compose   │    │  ViewModel  │    │   UseCase   │    │  Repository │
│    Screen   │◀───│  (StateFlow)│◀───│  (Business  │◀───│  Interface  │
│             │    │             │    │   Logic)    │    │             │
└──────┬──────┘    └─────────────┘    └─────────────┘    └──────┬──────┘
       │                                                       │
       │                                                       ▼
       │                                                ┌─────────────┐
       │                                                │  Repository │
       │                                                │    Impl     │
       │                                                └──────┬──────┘
       │                                                       │
       │                              ┌────────────────────────┼────────────────────────┐
       │                              │                        │                        │
       │                              ▼                        ▼                        ▼
       │                       ┌─────────────┐         ┌─────────────┐         ┌─────────────┐
       │                       │   Room DB   │         │  Encrypted  │         │   System    │
       │                       │  (Locked    │         │  SharedPrefs│         │   APIs      │
       │                       │   Apps)     │         │  (Password) │         │  (Biometric)│
       │                       └──────┬──────┘         └─────────────┘         └─────────────┘
       │                              │
       │                              ▼
       │                       ┌─────────────┐
       └───────────────────────│   DAO       │
                               │ (Queries)   │
                               └─────────────┘
```

---

## 3. Lock Type Selection & Validation Flow

```
┌─────────────┐
│ User Opens  │
│  Settings   │
└──────┬──────┘
       │
       ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ Select Lock │───▶│   PIN?      │───▶│ 4-6 Digit   │───▶│ Confirm PIN │
│   Type      │    │             │    │   Input     │    │  (2 times)  │
└─────────────┘    └─────────────┘    └─────────────┘    └──────┬──────┘
       │                                                        │
       │                    ┌─────────────┐    ┌─────────────┐   │
       │                    │  Pattern?   │───▶│ 3x3 Grid   │───┤
       │                    │             │    │ Draw Path  │   │
       │                    └─────────────┘    └─────────────┘   │
       │                                                        │
       │                    ┌─────────────┐    ┌─────────────┐   │
       │                    │ Password?   │───▶│ Alphanumeric│───┤
       │                    │             │    │   Input     │   │
       │                    └─────────────┘    └─────────────┘   │
       │                                                        │
       │                    ┌─────────────┐    ┌─────────────┐   │
       └───────────────────▶│ Biometric?  │───▶│ System      │───┘
                            │             │    │ BiometricPrompt│
                            └─────────────┘    └─────────────┘
                                                        │
                                                        ▼
                                               ┌─────────────┐
                                               │ Encrypt &   │
                                               │ Store Hash  │
                                               │ (MasterKey) │
                                               └──────┬──────┘
                                                      │
                                                      ▼
                                               ┌─────────────┐
                                               │  Save to    │
                                               │  Encrypted  │
                                               │  Prefs / DB │
                                               └──────┬──────┘
                                                      │
                                                      ▼
                                               ┌─────────────┐
                                               │ Setup Done  │
                                               │ Successful  │
                                               └─────────────┘
```

---

## 4. Uninstall Protection & Accessibility Blocker Flow

```
┌────────────────────────────────────────────────────────┐
│     User navigates to Settings > Apps > App Lock       │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│             Accessibility Service intercepts           │
│              window package state transition           │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
               Is Uninstall Intent / Panel?
               ┌────────────┴────────────┐
               │                         │
               ▼ YES                     ▼ NO
┌─────────────────────────────┐   ┌──────────────────────┐
│  Has active Device Admin?   │   │  Allow navigation    │
└──────────────┬──────────────┘   │  to standard screens │
               │                  └──────────────────────┘
        ┌──────┴──────┐
        │             │
        ▼ YES         ▼ NO
┌──────────────┐┌──────────────┐
│  Display     ││ Blocks app   │
│  Biometric/  ││ screen flow, │
│  PIN Overlay ││ triggers    │
└──────┬───────┘│ navigate HOME│
       │        └──────────────┘
       ▼
   Authorized?
   ┌───┴─────────────┐
   │                 │
   ▼ YES             ▼ NO
┌──────────────┐ ┌──────────────┐
│ Grant system │ │ Clear stack  │
│ settings access│ go back HOME │
└──────────────┘ └──────────────┘
```

---

## 5. Intruder Photo Capture Flow (CameraX Pipeline)

```
┌────────────────────────────────────────────────────────┐
│  User inputs pin, pattern, or password in LockActivity │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│          Hash mismatch or incorrect attempt            │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│             Increment wrong credentials counter        │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
     Wrong attempts count >= limit? (e.g. 2 attempts)
               ┌────────────┴────────────┐
               │                         │
               ▼ YES                     ▼ NO
┌─────────────────────────────┐   ┌──────────────────────┐
│  Check CameraX permissions  │   │  Increment fail counter│
└──────────────┬──────────────┘   │   and show shake UI    │
               │                  └──────────────────────┘
        ┌──────┴──────┐
        │             │
        ▼ GRANTED     ▼ DENIED
┌──────────────┐┌──────────────┐
│ Bind CameraX ││ Skip capture │
│ Lifecycle /  ││ Show standard│
│ Front camera ││ Shake error  │
└──────┬───────┘└──────────────┘
       │
       ▼
┌─────────────────────────────┐
│ Take silent photo snapshot  │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│ Save image to local cache   │
│ (AES-256 encrypted storage) │
└─────────────────────────────┘
```
