package com.fantone.app_saos.controller;


import com.fantone.app_saos.dto.request.*;
import com.fantone.app_saos.dto.response.*;
import com.fantone.app_saos.mapper.UserMapper;
import com.fantone.app_saos.model.*;
import com.fantone.app_saos.repository.*;
import com.fantone.app_saos.service.AuthService;
import com.fantone.app_saos.service.CardService;
import com.fantone.app_saos.service.MembershipService;
import com.fantone.app_saos.service.UserService;
import com.fantone.app_saos.service.payload.AuthTokens;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.LinkedHashMap;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class ApiController {
    private AuthService authService;
    private CardService cardService;
    private UserService userService;
    private UserMapper userMapper;
    private final ProductRepository productRepo;
    private final CardRepository cardRepo;
    private final GymPlanRepository gymPlanRepository;
    private final MembershipService membershipService;
    private final UserRepository userRepo;
    private final PurchasedProductRepository purchasedProductRepo;

    public ApiController(AuthService authService, CardService cardService, UserService userService, UserMapper userMapper, ProductRepository productRepo, CardRepository cardRepo, GymPlanRepository gymPlanRepository, MembershipService membershipService, UserRepository userRepo, PurchasedProductRepository purchasedProductRepo) {
        this.authService = authService;
        this.cardService = cardService;
        this.userService = userService;
        this.userMapper = userMapper;
        this.productRepo = productRepo;
        this.cardRepo = cardRepo;
        this.gymPlanRepository = gymPlanRepository;
        this.membershipService = membershipService;
        this.userRepo = userRepo;
        this.purchasedProductRepo = purchasedProductRepo;
    }

    private final Map<String, RateLimiter> userLimiters = new HashMap<>();

    private RateLimiter getLimiterForUser(Long userId, String endpoint) {
        String key = userId + ":" + endpoint; // es. "42:weather" o "42:weather_weekly"
        return userLimiters.computeIfAbsent(key, k -> {
            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod(5)
                    .limitRefreshPeriod(Duration.ofMinutes(1))
                    .timeoutDuration(Duration.ofMillis(0))
                    .build();
            return RateLimiterRegistry.of(config).rateLimiter(k);
        });
    }


    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse register(@RequestBody @Valid AuthRequestDto dto) {
        return authService.register(dto);
    }

    @PostMapping("/auth/login")
    public TokenJWTResponseDto login(@RequestBody @Valid LoginRequestDto dto) {
        AuthTokens tokens = authService.login(dto);

        return new TokenJWTResponseDto(
                        tokens.accessToken(),
                        tokens.refreshToken(),
                        "Bearer",
                        60 * 15, //15 minuti
                        2592000 //30 giorni
                );
    }

    @PostMapping("/auth/refresh")
    public RefreshTokenResponseDto refresh(@RequestBody @Valid RefreshJWTRequestDto dto) {
        String accessToken = authService.refresh(dto.refreshToken());

        return new RefreshTokenResponseDto(
                accessToken,
                "Bearer",
                2592000 //30 giorni
        );

    }

    @Value("${weather.api.key}")
    private String apiKey;

    @GetMapping("/weather/{city}")
    public ResponseEntity<?> getWeather(@PathVariable String city) {

        if (!city.matches("^[a-zA-Z\\s\\-]{1,50}$")) {
            return ResponseEntity.badRequest().body("Città non valida");
        }

        try {

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long userId = Long.valueOf(auth.getName());

            // ⛔ RATE LIMITING
            RateLimiter limiter = getLimiterForUser(userId, "weather");

            // 4️⃣ Controlla il rate limiter
            if (!limiter.acquirePermission()) {
                return ResponseEntity.status(429).body("Troppe richieste, riprova tra un minuto");
            }


            // ✅ SICURO
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://api.openweathermap.org/data/2.5/weather")
                    .queryParam("q", city)
                    .queryParam("appid", apiKey)
                    .queryParam("units", "metric")
                    .build()
                    .toUriString();

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            Map<String, Object> main = (Map<String, Object>) response.get("main");
            double temp = ((Number) main.get("temp")).doubleValue();

            List<Map<String, Object>> weatherList =
                    (List<Map<String, Object>>) response.get("weather");
            Map<String, Object> weather = weatherList.get(0);

            String condition = (String) weather.get("main");
            String description = (String) weather.get("description");

            return ResponseEntity.ok(new WeatherResponseDto(temp, condition, description, city));

        } catch (RestClientException e) {
            return ResponseEntity.status(500).body("Errore nel recupero meteo");
        }

    }


    @GetMapping("/weather/weekly/{city}")
    public ResponseEntity<?> getWeeklyWeather(
            @PathVariable String city) {

        if (!city.matches("^[a-zA-Z\\s\\-]{1,50}$")) {
            return ResponseEntity.badRequest().body("Città non valida");
        }

        try {

            // 🔐 AUTH
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long userId = Long.valueOf(auth.getName());

            // ⛔ RATE LIMITING
            RateLimiter limiter = getLimiterForUser(userId, "weather_weekly");
            if (!limiter.acquirePermission()) {
                return ResponseEntity.status(429).body("Troppe richieste, riprova tra un minuto");
            }

            // 🌐 API CALL OPENWEATHER (5 giorni / 3 ore)
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://api.openweathermap.org/data/2.5/forecast")
                    .queryParam("q", city)
                    .queryParam("appid", apiKey)
                    .queryParam("units", "metric")
                    .build()
                    .toUriString();

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                return ResponseEntity.status(500).body("Errore meteo");
            }

            List<Map<String, Object>> list =
                    (List<Map<String, Object>>) response.get("list");

            Map<String, List<Map<String, Object>>> groupedByDay = new LinkedHashMap<>();

            // 📅 GROUP BY DAY
            for (Map<String, Object> item : list) {

                String dtTxt = (String) item.get("dt_txt");
                String day = dtTxt.split(" ")[0]; // YYYY-MM-DD

                groupedByDay.putIfAbsent(day, new ArrayList<>());
                groupedByDay.get(day).add(item);
            }

            // 📦 BUILD DTO
            List<Map<String, Object>> forecast = new ArrayList<>();

            for (String day : groupedByDay.keySet()) {

                List<Map<String, Object>> dayItems = groupedByDay.get(day);

                List<Map<String, Object>> hourly = new ArrayList<>();

                for (Map<String, Object> item : dayItems) {

                    Map<String, Object> main = (Map<String, Object>) item.get("main");
                    List<Map<String, Object>> weatherList =
                            (List<Map<String, Object>>) item.get("weather");

                    Map<String, Object> weather = weatherList.get(0);

                    Map<String, Object> hour = new HashMap<>();
                    hour.put("time", ((String) item.get("dt_txt")).split(" ")[1].substring(0, 5));
                    hour.put("temperature", main.get("temp"));
                    hour.put("description", weather.get("description"));

                    hourly.add(hour);
                }

                Map<String, Object> dayMap = new HashMap<>();
                dayMap.put("date", day);
                dayMap.put("hourlyForecasts", hourly);

                forecast.add(dayMap);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("city", city);
            result.put("forecast", forecast);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Errore meteo per città '{}': {}", city.replaceAll("[\r\n]", "_"), e.getMessage());
            return ResponseEntity.status(500).body("Errore nel recupero meteo settimanale");
        }
    }



    // Fallback chiamato se il rate limit è superato
//    public ResponseEntity<String> rateLimitFallback(String city, Throwable t) {
//        return ResponseEntity.status(429)
//                .body("Troppe richieste, riprova tra un minuto");
//    }

    @PostMapping("/card/generate")
    public ResponseEntity<CardDto> generateCard(@AuthenticationPrincipal String userId) {
        CardDto card = cardService.generateCard(Long.parseLong(userId));
        return ResponseEntity.status(HttpStatus.CREATED).body(card);
    }

    @PostMapping("/card/recharge")
    @Transactional
    public ResponseEntity<?> rechargeCard(@RequestBody @Valid RechargeCardRequestDto payload, @AuthenticationPrincipal String userId) {

        // 1. Estrazione dell'importo dal corpo della richiesta
        BigDecimal amount = payload.amount();

        // 2. Recupero della tessera attiva (non scaduta)
        Optional<Card> cardOpt = cardRepo.findByUserIdAndExpiresAtAfter(Long.parseLong(userId), LocalDateTime.now());
        if (cardOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Nessuna tessera attiva trovata per questo utente."));
        }
        Card card = cardOpt.get();

        // 3. Aggiornamento del saldo
        card.setBalance(card.getBalance().add(amount));
        Card savedCard = cardRepo.save(card);

        // 4. Risposta con i dati aggiornati della card (usando il mapper o restituendo i campi necessari)
        return ResponseEntity.ok(new CardDto(
                savedCard.getId(),
                savedCard.getBalance(),
                savedCard.getExpiresAt(),
                savedCard.getCreatedAt()
        ));

    }

    @GetMapping("/card/mycard")
    public ResponseEntity<CardDto> getMyCard(@AuthenticationPrincipal String userId) {
        return cardService.findByUserId(Long.parseLong(userId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping("/user/me")
    public UserDto getMe(@AuthenticationPrincipal String userId) {
        User user = userService.findById(Long.parseLong(userId));
        return userMapper.toDto(user);
    }

    @GetMapping("/user/shop/history")
    public ResponseEntity<List<PurchaseHistoryDto>> getOrderHistory(@AuthenticationPrincipal String userId) {

        Long parsedUserId = Long.parseLong(userId);

        List<PurchasedProduct> purchases =
                purchasedProductRepo
                        .findByUserIdOrderByPurchasedAtDesc(parsedUserId);

        List<PurchaseHistoryDto> response = purchases.stream()
                .map(p -> new PurchaseHistoryDto(
                        p.getProductName(),
                        p.getPrice(),
                        p.getQuantity(),
                        p.getPrice() * p.getQuantity(),
                        p.getPurchasedAt()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }


    @GetMapping("/products")
    public List<ProductInfoDto> getProducts() {
        return productRepo.findAll().stream()
                .map(p -> new ProductInfoDto(
                        p.getId(),
                        p.getName(),
                        p.getPrice(),
                        p.getStockQuantity()
                ))
                .toList();

    }

    // Carica tutti i piani disponibili nello shop
    @GetMapping("/plans")
    public ResponseEntity<List<GymPlan>> getAllPlans() {
        List<GymPlan> plans = gymPlanRepository.findAll();
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/subscription/mysub")
    public ResponseEntity<MembershipResponseDto> getMySubscription(@AuthenticationPrincipal String userId) {

        MembershipResponseDto activeSub = membershipService.getActiveSubscriptionByUserId(Long.parseLong(userId));

        if (activeSub == null) {
            return ResponseEntity.notFound().build(); // Il JS interpreterà il 404 mostrando il box "Acquista"
        }

        return ResponseEntity.ok(activeSub);
    }

    @PostMapping("/subscription/buy")
    public ResponseEntity<?> buySubscription(@AuthenticationPrincipal String userId, @RequestBody Map<String, Long> payload) {

        Long planId = payload.get("planId");
        if (planId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "ID del piano mancante"));
        }

        try {
            membershipService.purchasePlan(Long.parseLong(userId), planId);
            return ResponseEntity.ok().build();
        } catch (com.fantone.app_saos.exception.ResourceConflictException e) {
            // Cattura specificamente l'eccezione lanciata dal Service (saldo insufficiente, card mancante, ecc.)
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            // Qualsiasi altro errore imprevisto di sistema
            return ResponseEntity.internalServerError().body(Map.of("message", "Errore del server durante l'acquisto"));
        }
    }

    @PostMapping("/shop/checkout")
    @Transactional // Fondamentale: o tutto il carrello o niente
    public ResponseEntity<?> checkout(
            @RequestBody CheckoutRequestDto request,
            @AuthenticationPrincipal String userId) {

        Long parsedUserId = Long.parseLong(userId);

        // 1. Recupera l'utente loggato dal database per associarlo all'acquisto
        User user = userRepo.findById(parsedUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non trovato"));

        // 2. Recupera la tessera dell'utente
        Card card = cardRepo.findByUserIdAndExpiresAtAfter(parsedUserId, LocalDateTime.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tessera non trovata o non attiva"));

        // Inizializziamo il totale a ZERO
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 3. Prima passata: Validazione Stock e Calcolo Prezzo (Server-side)
        for (CartItemDto item : request.items()) {
            Product product = productRepo.findById(item.id())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prodotto non trovato: ID " + item.id()));

            if (product.getStockQuantity() < item.quantity()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Stock insufficiente per il prodotto: " + product.getName()));
            }

            // Calcolo: prezzo * quantità
            BigDecimal itemTotal = BigDecimal.valueOf(product.getPrice())
                    .multiply(BigDecimal.valueOf(item.quantity()));
            totalAmount = totalAmount.add(itemTotal);
        }

        // 4. Verifica Saldo della Tessera
        if (card.getBalance().compareTo(totalAmount) < 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Saldo insufficiente sulla tessera. Totale richiesto: €" + totalAmount));
        }

        // 5. Seconda passata: Aggiornamento dati, scarico magazzino e SALVATAGGIO STORICO ACQUISTI
        for (CartItemDto item : request.items()) {
            Product product = productRepo.findById(item.id()).get();

            // Riduciamo lo stock del prodotto nel catalogo shop
            product.setStockQuantity(product.getStockQuantity() - item.quantity());
            productRepo.save(product);

            // SALVA IL PRODOTTO ACQUISTATO NEL DATABASE
            PurchasedProduct purchase = new PurchasedProduct();
            purchase.setUser(user);
            purchase.setProductName(product.getName());
            purchase.setPrice(product.getPrice());
            purchase.setQuantity(item.quantity());
            purchase.setPurchasedAt(LocalDateTime.now());

            purchasedProductRepo.save(purchase);
        }

        // 6. Detrazione finale dal saldo della tessera
        card.setBalance(card.getBalance().subtract(totalAmount));
        cardRepo.save(card);

        // 7. Risposta di successo
        return ResponseEntity.ok(Map.of(
                "message", "Acquisto completato con successo!",
                "totalSpent", totalAmount,
                "newBalance", card.getBalance()
        ));
    }


}
