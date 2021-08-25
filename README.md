# Lab 3 - Integrera med andra tjänster

## Syfte
Syftet med denna lab är att få en inblick i hur man integrerar med andra tjänster, både när det kommer till att skriva kod men även hur man säkerställer att all fungerar så som det ska.

## Föregående labbar
Vi har ni en flernivå-app som är paketerad med docker och docker-compose. Använd din app även i denna labb men om det skulle vara så att du inte har gjort tidigare labbar hittar du här ett implementationsförslag som du kan använda.

## Övergripande målbild
Efter slutförd lab ska din applikation kunna:
* Neka en användare från att skapa ett konto om den inte klarar en kreditupplysning.
* Säkerställa att applikationen gör så som den ska genom enhetstester samt integrationstester.

## Ramverk
* Wiremock
* Testcontainers
* Junit5
* Mockito
* MockMVC
* RestTemplate


# Del 1: Utforska risktjänsten med hjälp av swagger
Börja med att lägga till risktjänsten i din `docker-compose.yml` dess image namn är `daneidmark/risk:0.0.1` och har ingen konfiguration. Den exponerar porten `8080`så du behöver mappa om den till en fri port, tex `8082` går bra så länge den är fri.

Använd det exponerade swagger UIt för att lista ut hur risktjänsten fungerar.
Swagger är ett verktyg för att dokumentera APIer. Om du vill kan du lägga till det i din tjänst men det är inget krav. Det du bör göra är att läsa lite om det, men framförallt använda dig av UIt för att lista ut hur risktjänsten fungerar.
Om du mappade om porten till `8082` kan du nu hitta swagger på `http://localhost:8082/swagger-ui`. Klicka runt och testa lite anrop

# Del 2: Integrera mot tjänsten
Skapa en `Adapter` som gör det externa anropet. Fördelen med en adapter är att vi kan mocka den i våra enhetstest samt att vi kan byta up hur vi för riskbedömningar utan att anropande kod vet om det.
`Adapter Pattern` är händigt om du vill kapsla in något och vi använder det ofta till externa tjänster eller saker med sidoeffekter. 

Skapa ett interface i ditt domänlager som uttrycker hur du vill jobba med riskbedömningar. Tex, vilken data vill du tillhandahålla? hur vill du har resultatet? 
Implementera ditt interface. Här kan du nu översätta din input till ett rest-anrop samt ta responset från rest-anropet och översätta till det formatet du vill. Här vill vi inte läcka ut några tekniska detaljer eller annat som är relaterat till risktjänsten som du inte är intresserad av i din domän.

Som restklient föreslår jag att du använder `RestTemplate` då den är förhållandevis enkel. Tänk även på att göra detta testbart. Vi vill tex kunna byta url till rikstjänsten via testfall. Ett förslag är att ha den urlen som en property i din konfigurationsfil.

# Del 4: Testa

* Skapa ett Unittest som testar din applikationstjänst. Tex `PersonalFinanceService#openAccount` i exempel applikationen.
Använd mockito för att mocka både adaptern och repositoryt.
Du kan specifiera en mock med hjälp av: `MyRepository myRepository = mock(MyRepository.class)`
Därefter kan du använda `when()` för att berätta hur den ska bete sig vid androp samt `verify()`för att asserta att den rätt saker har anropats på rätt sätt.

* Skapa ett integrationstest för din adapter. Använd Wiremock för att stubba alla REST request.
Wiremock är ett bra verktyg om du även vill testa din http-klient och att du kan serializera och deserializera data på rätt sätt.
Det wiremock för är att den spinner upp en fake-tjänst som tar emot ditt anrop och svara med det du säger att den ska svara. Det är ett bra verktyg!

Du kan skapa wiremock genom att bara instansiera den med `WireMockServer wiremock = new WireMockServer();` dock så måste vi köra lite fler saker. Vi vill inte att ett test ska påverka resultatet av ett annat test. Så det man brukar göra är att implementera @BeforeEach och @AfterEach i JUnit. Dessa två annotationer, om de sätts på en metod kommer de alltid att köras innan och efter varje testfall.
Det vi kan göra då är i BeforeEach starta wiremock och i AfterEach stoppa. Då har vi alltid en fräsch wiremock vid varje test!

```
    @BeforeEach
    public void before() {
        wireMockServer.start();
    }

    @AfterEach
    public void after() {
        wireMockServer.stop();
    }
```

Nästa steg vi behöver göra är att konfigurera wiremock. Vi behöver berätta för den att när du får ett request på detta format så ska du returnera ett repsonse på detta format.

Detta kan ni läsa er till i wiremocks dockumentation men det kommer se ut ungeför såhär:
```
 wireMockServer.stubFor(<urlpattern>)
                .willReturn(aResponse()
                    .withStatus(<status>)
                    .withHeader(<header>)
                    .withBody(<body>));
```


Använd MockMVC och testa från Controllern. Säkertäll att ett konto har skapats och att balansen är 0.
För att slippa starta en lokal databas varje gång vi ska köra ett test så kan vi använda oss av testcontainers
Då kan vi spinna upp en lokal databas just för testfallet så att vi inte geggar ner våran lokala databas med testdata.
Familjera dig med `Lab1ApplicationTests.java`

`@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` kommer starta din app på en random port.
`@ExtendWith(SpringExtension.class)` kommer ge dig tillgång till alla Beans
`@ContextConfiguration(initializers = Lab1ApplicationTests.Lab1ApplicationTestsContextInitializer.class)` Ger dig möjlighet att skriva över konfiguration. Tex vi vill nu peka ut en ny databas url för detta testfall.
`@Testcontainers` aktiverar testcontainers för denna klass
`@Container private static final MySQLContainer db = new MySQLContainer("mysql:8.0.26").withPassword("password");` startar en lokal mysql för denna klass.
`@AutoConfigureMockMvc` Skapar upp en MockMvc som är rätt konfigurerad för din app.

```
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@Testcontainers
@ContextConfiguration(initializers = Lab1ApplicationTests.Lab1ApplicationTestsContextInitializer.class)
@AutoConfigureMockMvc
class Lab1ApplicationTests {

    @Container
    private static final MySQLContainer db = new MySQLContainer("mysql:8.0.26").withPassword("password");


    @Test
    void contextLoads() {
    }

    @Test
    void shouldOpenAccountWithZeroBalance() {
        // TODO
    }

    public static class Lab1ApplicationTestsContextInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            String host = db.getJdbcUrl();
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    configurableApplicationContext,
                    "spring.datasource.url=" + host, "flyway.url=" + host);

        }
    }
}

```

Även detta testfall behöver vi konfigurera wiremock.

## Optional: Skriv ett integrationstest mot databasen
För att säkerställa att Hibernate entityn är skapad på rätt sätt samt att alla egenskrivna sql-statements fungerar så som de ska, är det en bra sak att skriva integrationstest mot databasen. Det finns ett kodskelett i `AccountRepositoryHibernateIT` Kolla igenom det och se till att ni förstår vad som händer.

## Del 4 Optional: Implementera retries
Om anropen failar vill vi testa igen. Det är bra med ett self-healing-system, dvs ett system som lagar sig självt. Det kan vara så att det går att göra andropet direkt efter.
Vi måste fundera på ett par saker
1. Om tråden är skapad av en användare kan vi inte retrya föralltid då blir det bara en spinner i guit.
2. Om vi retryar för ofta kan vi tillslut förstöra för den andra tjänsten genom att DoS-attackera den.

Implementera "Exponential backoff". Dvs att vi väntar längre och längre mellan varje retry.
För att undvika att flera instancer hamnar i samma retry period, vill vi även introducera ett jitter. Dvs en variabel tidsförskjutning.

Formeln som jag vill att ni ska använda är
```
delay = (base*2^n) +- jitter
base = initiala väntetiden
n = antal försök
jitter = random tid (-X, +X) där ni själva får hitta på ett bra värde.

pseudo code:
retries = 0
while retries < 3
    svar = gör_rest_anrop
    if svar == error
        wait base*2^retries + jitter
    else
       return svar.body
```

## Del 5 Optional: Circuit breakers och Bulk heads. Kan vara ganska svårt!
Läs på om Circuit breaker och/eller Bulk Heads och testa att implementera en själv, alternativ använd ett bibliotek (tex Hysterix, Resillient4j)
