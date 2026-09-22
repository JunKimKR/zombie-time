# 🧟 좀비타임 (ZombieTime)

SNS를 볼수록 귀여운 캐릭터가 좀비로 변해가는 안드로이드 앱.

인스타그램 · 스레드 · 페이스북 · 유튜브의 **화면에 실제로 떠 있던 시간**을 모두 합쳐서,
하루 목표(기본 3시간)에 가까워질수록 캐릭터가 사람 → 좀비로 연속적으로 변합니다.

![좀비화 단계](docs/character-stages.png)

---

## 주요 기능

| 기능 | 설명 |
|---|---|
| 스크린타임 수집 | `UsageStatsManager` 이벤트 기반 포그라운드 시간 집계 (백그라운드 재생 제외) |
| 상시 배너 | 포그라운드 서비스 + 상시 알림. 잠금화면/알림창에 캐릭터 그림 + 단계 + 사용시간 + 진행바를 1분마다 갱신 |
| 단계 알림 | 인간 → 좀비 6단계를 넘어갈 때마다 캐릭터 대사와 함께 푸시 |
| 앱별 상세 | 인스타/스레드/페북/유튜브 각각 사용시간 막대그래프 |
| 주간 통계 | 최근 7일 막대그래프 + 4주 좀비 캘린더 + 앱별 주간 합계 |
| 하루 브리핑 | 매일 밤(기본 22시) 1080×1920 리포트 카드를 만들어 **인스타 스토리로 바로 공유** |
| 설정 | 목표 시간(1~8시간), 브리핑 시각, 알림 on/off |

캐릭터는 이미지 파일이 아니라 `CharacterRenderer` 가 **캔버스에 직접 그리는 벡터**라서
화면·알림 아이콘·스토리 카드가 전부 같은 그림을 어떤 해상도로든 선명하게 씁니다.

### 하루 브리핑 카드 (인스타 스토리용)

<img src="docs/briefing-card.png" width="300">

---

## 빌드 및 설치

Android Studio에서 프로젝트를 열거나 JDK 17과 Android SDK 36 환경에서 실행합니다.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease
```

AGP 8.10.1 / Gradle 8.11.1을 사용합니다. GitHub Actions의 `zombietime-preview` 아티팩트에는
설치용 debug APK와 서명 전 release AAB가 포함됩니다. release에 debug 키를 사용하지 않으며,
Play 제출 전 production 업로드 키로 별도 서명해야 합니다. main 빌드 성공 시 `v1.1.0-preview.1` 테스트 릴리스를 한 번 게시합니다.

---

## 설치 후 첫 실행

앱이 두 가지 권한을 요청합니다.

1. **사용 정보 접근** — 안드로이드 설정 화면이 열리면 목록에서 `좀비타임`을 찾아 켜주세요.
   (이 권한은 시스템 설정에서만 켤 수 있어서 팝업으로 처리할 수 없습니다.)
2. **알림** — 상시 배너를 띄우기 위해 필요합니다.

둘 다 켜면 바로 배너가 뜨고 캐릭터가 살아납니다.

> 삼성/샤오미 등 일부 기기는 배터리 최적화가 서비스를 종료시킬 수 있습니다.
> 배너가 사라지면 `설정 → 배터리 → 좀비타임 → 제한 없음` 으로 바꿔주세요.

---

## 좀비화 단계

| 단계 | 목표 대비 | 이름 |
|---|---|---|
| 0 | 0~10% | 말짱한 사람 |
| 1 | 10~30% | 살짝 몽롱 |
| 2 | 30~50% | 눈이 풀림 |
| 3 | 50~72% | 피부가 초록 |
| 4 | 72~92% | 거의 좀비 |
| 5 | 92%+ | 완전 좀비 |

---

## 프로젝트 구조

```
app/src/main/java/com/zombietime/app/
├── MainActivity.kt              화면 전환 · 상태 관리 · 권한 요청
├── ZombieApp.kt                 Application (채널/알람 초기화)
├── Notifications.kt             상시 배너 · 단계 알림 · 브리핑 알림
├── character/
│   └── CharacterRenderer.kt     인간→좀비 벡터 캐릭터 드로잉 (핵심)
├── data/
│   ├── Models.kt                추적 대상 앱 · 시간 포맷
│   ├── ZombieStage.kt           단계 정의 · 대사
│   ├── Prefs.kt                 설정 및 일별 기록 저장
│   └── UsageRepository.kt       UsageStatsManager 집계
├── service/
│   ├── ZombieMonitorService.kt  1분 주기 갱신 포그라운드 서비스
│   ├── BootReceiver.kt          재부팅 후 자동 시작
│   ├── BriefingAlarm.kt         매일 브리핑 알람 예약
│   └── DailyBriefingReceiver.kt 브리핑 발송
├── share/
│   ├── StoryImageBuilder.kt     1080×1920 스토리 카드 생성
│   └── ShareHelper.kt           인스타 ADD_TO_STORY + 폴백 공유
└── ui/                          Compose 화면 (홈/주간/설정/온보딩/브리핑)
```

## 개인정보

모든 사용 기록은 기기 내 `SharedPreferences` 에만 저장됩니다.
네트워크 권한 자체가 없어서 어디로도 전송되지 않습니다.

## 1.1 회복 업데이트

홈에서 캐릭터를 눌러 인사하고, 새 **쉼터** 탭에서 5·15·25분 쉬어보세요.
완료할 때마다 무료 씨앗을 모아 숲/달빛 배경을 열 수 있습니다. 누적 휴식과 연속 기록,
배지도 기기에 저장됩니다. 자율 타이머이며 다른 앱을 차단하거나 SNS 기록을 줄이지 않습니다.

빌드 환경은 **JDK 17 / AGP 8.10.1 / Gradle 8.11.1 / Android SDK 36**입니다.
Actions의 `zombietime-preview`에서 테스트용 debug APK와 **미서명** release AAB를 받습니다.
기존 `latest` 자동 배포와 debug 키 release 서명은 중단했습니다.
Play 제출에는 별도 production 서명과 Console 설정이 필요합니다.
세부 출시/결제 계획과 기기 테스트 항목은 [RELEASE_PLAN.md](docs/RELEASE_PLAN.md)를 참고하세요.

테스트 APK는 `com.zombietime.app.preview` / **좀비타임 테스트**로 별도 설치됩니다.
설치와 테스트 항목: [TEST_RELEASE.md](docs/TEST_RELEASE.md)
