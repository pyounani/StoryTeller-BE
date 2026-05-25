<img src="https://github.com/user-attachments/assets/cdd38273-38eb-4fd8-b313-e8b4e5f44857"/>

## STORYTELLER

`#사용자 맞춤형 동화 생성` `#학습에 편리한 기능` <br /> <br />
기존에 없는 새로운 영어 동화를 위해 원하는 키워드를 통해 다양하고 창의적인 영어 동화를 경험을 통해, <br /> 영어 동화에 대한 어린이들의 흥미와 관심을 유발할 수 있는 <br />
**"어린이들을 위한 창의적인 영어 동화 앱"**, STORYTELLER입니다.

> 2024 ICT멘토링 한이음 공모전 - 입선🏆<br />
> 개발 기간: 2024.02 ~ 2024.11

![편집본  StoryTeller 배속 자막 편집본 4K](https://github.com/user-attachments/assets/61a1313c-95c0-4f5f-b9a8-cf1d00a59f3c) <br />
[Go to YouTube Link](https://www.youtube.com/watch?v=9JHMy-bQO-Y)

<br />

## Backend Focus
단순 기능 구현보다 **실제 운영 과정에서 발생할 수 있는 문제를 직접 분석하고 개선**하는 과정에 집중했습니다.

- 이메일 인증 비동기 처리 안정성 개선
- S3와 DB 간 데이터 정합성 문제 대응
- Tomcat Thread Pool 및 DBCP 튜닝
- 주요 API 쿼리 성능 최적화

<br />

### Technical Challenges

#### 1. 데이터 기반 리소스 튜닝을 통한 API Latency 개선 및 안정화
- JMeter 기반 부하 테스트와 JVM 스레드 상태 분석을 통해 병목 원인을 추적하고, DBCP(HikariCP) 및 Thread Pool을 데이터 기반으로 튜닝하여 Peak Latency를 438ms → 258ms로 개선했습니다.

→ 자세한 분석은 [Blog](https://docs.google.com/spreadsheets/d/1zEjwYSY-NhqJQbnz3dk-hgAdbkbbq5OJWCX8PXbl7E4/edit) 참고
<br /> 
<br />

#### 2. 이메일 발송 안정성 개선을 위한 Retry 정책 및 비동기 스레드 풀 튜닝
- 불필요한 재시도를 방지하는 Custom Retry 정책을 적용하고,  SMTP 제약 환경을 모사한 Mock 서버 기반 부하 테스트를 통해 비동기 스레드 풀 및 큐 설정을 최적화했습니다.

→ Retry 분석은 [Blog](https://pyounani.tistory.com/26) 참고 <br />
→ 부하 테스트 분석은 [Blog](https://pyounani.tistory.com/29) 참고

<br />

#### 3. DB 트랜잭션과 S3 간 데이터 정합성 보장을 위한 구조 설계
- 트랜잭션 롤백 시 DB와 S3 간 데이터 불일치 문제가 발생할 수 있음을 확인하고, 삭제 요청을 별도 테이블에 기록한 뒤 배치 기반으로 처리하는 보상 구조를 도입하여 데이터 정합성을 개선했습니다.

→ 자세한 분석은 [Blog](https://pyounani.tistory.com/27) 참고

<br />


### Tech stack

#### 1. Service Architecture
![서비스 구성도](https://github.com/user-attachments/assets/24059f87-4542-4cb2-989e-bf710dde687e)

#### 2. Infrastructure Architecture
![아키텍처 설계도](https://github.com/user-attachments/assets/1ed56472-6ca7-44d2-ad1b-709ea534aeee)

#### Environment
![Git](https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=Git&logoColor=white)
![Github](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=GitHub&logoColor=white)             

#### FrontEnd
![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=Javascript&logoColor=white)
![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)

#### BackEnd
![SpringBoot](https://img.shields.io/badge/springboot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![SpringSecurity](https://img.shields.io/badge/springsecurity-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![MySQL](https://img.shields.io/badge/Mysql-4479A1?style=for-the-badge&logo=mysql&logoColor=white)

#### Server
![AmazonRDS](https://img.shields.io/badge/amazonrds-527FFF?style=for-the-badge&logo=amazonrds&logoColor=white)
![AmazonRDS](https://img.shields.io/badge/amazonec2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white)
![AmazonS3](https://img.shields.io/badge/amazons3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)

#### Communication
![Slack](https://img.shields.io/badge/Slack-4A154B?style=for-the-badge&logo=Slack&logoColor=white)
![Notion](https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=Notion&logoColor=white)
![GoogleMeet](https://img.shields.io/badge/GoogleMeet-00897B?style=for-the-badge&logo=Google%20Meet&logoColor=white)
![GoogleMeet](https://img.shields.io/badge/Jira-0052CC?style=for-the-badge&logo=Jira%20Meet&logoColor=white)


