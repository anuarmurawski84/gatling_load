package webtours;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class WebToursSimulations extends Simulation {

    private String pattern = "yyyy-MM-dd";
    private String patternTom = "MM/dd/yyyy";

    String dateInString = new SimpleDateFormat(pattern).format(new Date());
    LocalDate parsedDate = LocalDate.parse(dateInString);
    LocalDate addedDate = parsedDate.plusDays(1);
    LocalDate addedTwoDate = parsedDate.plusDays(2);
    String tomDate = addedDate.format(DateTimeFormatter.ofPattern(patternTom));
    String afterTomDate = addedTwoDate.format(DateTimeFormatter.ofPattern(patternTom));


    Iterator<Map<String, Object>> feederTomDate =
            Stream.generate((Supplier<Map<String, Object>>) () -> {
                        return Collections.singletonMap("tomDate", tomDate);
                    }
            ).iterator();

    Iterator<Map<String, Object>> feederAfterTomDate =
            Stream.generate((Supplier<Map<String, Object>>) () -> {
                        return Collections.singletonMap("afterTomDate", afterTomDate);
                    }
            ).iterator();

    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://webtours.load-test.ru:1080");

    private static FeederBuilder.FileBased<String> departureCityFeeder = csv("data/departureCity.csv")
            .random();

    private static FeederBuilder.FileBased<String> credentialsFeeder = csv("data/credentials.csv")
            .random();

    private static FeederBuilder.FileBased<String> arrivalCityFeeder = csv("data/arrivalCity.csv")
            .random();

    private static FeederBuilder.FileBased<String> flightFeeder = csv("data/flight.csv")
            .random();

    private static ChainBuilder getWebtoursStartPage =
            exec(http("Get webtours starting page")
                    .get("/webtours")
                    .check(status().is(200))
            );

    private static ChainBuilder getHeader =
            exec(http("Get webtours header page")
                    .get("/webtours/header.html")
                    .check(status().not(404), status().not(500))
            );
    private static ChainBuilder getWelcome =
            exec(http("Get webtours starting page")
                    .get("/cgi-bin/welcome.pl")
                    .queryParam("signOff", "true")
                    .check(status().is(200))
                    );
    private static ChainBuilder getUserId =
            exec(http("Get userId")
                    .get("/cgi-bin/nav.pl")
                    .queryParam("in", "home")
                    .check(css("input[name=\"userSession\"]","value").findRandom().saveAs("userSession"))
                    .check(status().is(200)))
                    .exec(
                            session -> {
                                System.out.println("Response body: " + session.getString("userSession"));
                                System.out.println("Tomorrow date: " + session.getString("tomDate"));
                                System.out.println("AfterTomorrow date: " + session.getString("afterTomDate"));

                                return session;
                            }
                    );

    private static ChainBuilder getHome =
            exec(http("Get webtours home page")
                    .get("/WebTours/home.html")
                    .check(status().is(200))
            );
    private static ChainBuilder getMenu =
            exec(http("Get webtours starting page after")
                    .get("/cgi-bin/login.pl")
                    .queryParam("intro", "true")
                    .check(status().is(200))
                    );

    private static ChainBuilder authenticate =
            exec(http("Authenticate")
                    .post("/cgi-bin/login.pl")
                            .formParam("userSession", "#{userSession}")
                            .formParam("username", "#{login}")
                            .formParam("password", "#{password}")
                            .formParam("login.x", "0")
                            .formParam("login.y", "0")
                            .formParam("JSFormSubmit", "off")
                    .check(status().is(200))
            );

    private static ChainBuilder getHomeAfterAction =
            exec(http("Get webtours starting page")
                    .get("/cgi-bin/nav.pl")
                    .queryParam("page", "menu")
                    .queryParam("in", "home")
                    .check(status().is(200))
            );
    private static ChainBuilder getFlight =
            exec(http("Get webtours starting page")
                    .get("/cgi-bin/nav.pl")
                    .queryParam("page", "menu")
                    .queryParam("in", "flights")
                    .check(status().is(200))
            );

    private static ChainBuilder getReservations =
            exec(http("Get reservations page")
                    .get("/cgi-bin/reservations.pl")
                    .queryParam("page", "welcome")
                    .check(status().is(200))
            );

    private static ChainBuilder postFlight =
            exec(http("postFlight request")
                            .post("/cgi-bin/reservations.pl")
                    .formParam("advanceDiscount", "0")
                    .formParam("depart", "#{departureCity}")
                    .formParam("departDate", "#{tomDate}")
                    .formParam("arrive", "#{arrivalCity}")
                    .formParam("returnDate", "#{afterTomDate}")
                    .formParam("numPassengers", "1")
                    .formParam("seatPref", "None")
                    .formParam("seatType", "Coach")
                    .formParam("findFlights.x", "36")
                    .formParam("findFlights.y", "4")
                    .formParam(".cgifields", "roundtrip")
                    .formParam(".cgifields", "seatType")
                    .formParam(".cgifields", "seatPref")
                            .check(css("input[type=\"radio\"]","value").findRandom().saveAs("flightValue"))
                            .check(status().is(200))

            )
                    .exec(
                            session -> {
                                System.out.println("Response body: " + session.getString("flightValue"));

                                return session;
                            }
                    );

    private static ChainBuilder postSelectFlightCost =
            exec(http("SelectFlightCost request")
                    .post("/cgi-bin/reservations.pl")
                    .formParam("outboundFlight", "#{flightValue}")
                    .formParam("numPassengers", "1")
                    .formParam("advanceDiscount", "0")
                    .formParam("seatType", "Coach")
                    .formParam("seatPref", "None")
                    .formParam("reserveFlights.x", "16")
                    .formParam("reserveFlights.y", "3")
                    .check(status().is(200))
            )
            ;

    private static ChainBuilder postFlightInvoice =
            exec(http("postFlightInvoice request")
                    .post("/cgi-bin/reservations.pl")
                    .formParam("firstName", "#{firstName}")
                    .formParam("lastName", "#{lastName}")
                    .formParam("address1", "#{address1}")
                    .formParam("address2", "#{address2}")
                    .formParam("pass1", "#{firstName} #{lastName}")
                    .formParam("creditCard", "#{creditCard}")
                    .formParam("expDate", "#{expDate}")
                    .formParam("oldCCOption", "")
                    .formParam("numPassengers", "1")
                    .formParam("seatPref", "None")
                    .formParam("seatType", "Coach")
                    .formParam("outboundFlight", "#{flightValue}")
                    .formParam("advanceDiscount", "0")
                    .formParam("returnFlight", "")
                    .formParam("JSFormSubmit", "off")
                    .formParam("buyFlights.x", "57")
                    .formParam("buyFlights.y", "1")
                    .formParam(".cgifields", "saveCC")
                    .check(status().is(200))
            );

    private static ChainBuilder postFlightAck =
            exec(http("postFlightAck request")
                    .post("/cgi-bin/reservations.pl")
                    .body(StringBody(
                            "{\n" +
                                    "  \"Book Another.x\": 11\n" +
                                    "  \"Book Another.y\": 7\n" +
                                    "}"
                    ))
                    .check(status().is(200))
            );

    private static ChainBuilder getMenusPage =
            exec(http("Get menus page")
                    .get("/cgi-bin/welcome.pl?page=menus")
                    .check(status().is(200))
            );

    private ScenarioBuilder scn = scenario("Webtours")
            .forever().on(
            exec(getWebtoursStartPage)
            .pause(2)
            .exec(getHeader)
            .exec(getWelcome)
            .feed(feederTomDate)
            .feed(feederAfterTomDate)
            .exec(getUserId)
            .pause(2)
            .exec(getHome)
            .pause(2)
            .feed(credentialsFeeder)
            .exec(authenticate)
            .exec(getHomeAfterAction)
            .pause(2)
            .exec(getMenu)
            .pause(2)
            .exec(getFlight)
            .pause(2)
            .exec(getReservations)
            .pause(2)
            .feed(departureCityFeeder)
            .feed(arrivalCityFeeder)
            .exec(postFlight)
            .pause(2)
            .exec(postSelectFlightCost)
            .pause(2)
            .exec(postFlightInvoice)
            .pause(2)
            .exec(getMenusPage)
            .pause(2)
            .exec(getHomeAfterAction)
            .pause(2)
            .exec(getMenu)
    );

    {
        setUp(
                scn.injectOpen(atOnceUsers(1))
//                scn.injectOpen(
//                        nothingFor(3),
//                        incrementUsersPerSec(1)
//                                .times(10)
//                                .eachLevelLasting(20)
//                                .separatedByRampsLasting(5)
//                                .startingFrom(1)
//        )
        .protocols(httpProtocol))
                .maxDuration(360)
                .assertions((global().responseTime().max().lt(10000)));
    }
}