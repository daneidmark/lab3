package se.nackademin.java20.lab1;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import se.nackademin.java20.lab1.application.PersonalFinanceService;
import se.nackademin.java20.lab1.domain.AccountRepository;
import se.nackademin.java20.lab1.domain.RiskAssesment;
import se.nackademin.java20.lab1.persistance.AccountRepositoryHibernate;
import se.nackademin.java20.lab1.risk.RestRiskAssessment;

import javax.persistence.EntityManager;

@Configuration
public class ApplicationConfiguration {
    @Value("${app.risk-service-url}")
    private String riskServiceBaseUrl;

    @Bean
    public AccountRepository accountRepository(EntityManager em) {
        return new AccountRepositoryHibernate(em);
    }

    @Bean
    public PersonalFinanceService personalFinanceService(AccountRepository accountRepository, RiskAssesment riskAssessment) {
        return new PersonalFinanceService(accountRepository, riskAssessment);
    }

    @Bean
    public RiskAssesment riskAssesment() {
        return new RestRiskAssessment(new RestTemplate(), riskServiceBaseUrl);
    }
}
