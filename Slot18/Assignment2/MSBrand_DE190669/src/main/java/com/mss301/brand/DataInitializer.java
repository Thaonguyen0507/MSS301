package com.mss301.brand;

import com.mss301.brand.entity.BrandWatch;
import com.mss301.brand.entity.WatchBrand;
import com.mss301.brand.repository.BrandRepository;
import com.mss301.brand.repository.BrandWatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private final BrandRepository brandRepository;
    private final BrandWatchRepository brandWatchRepository;

    @Override
    public void run(String... args) {
        if (brandRepository.count() == 0) {
            brandRepository.saveAll(List.of(
                brand("Rolex", "Switzerland"),
                brand("Omega", "Switzerland"),
                brand("Seiko", "Japan"),
                brand("Casio", "Japan"),
                brand("TAG Heuer", "Switzerland")
            ));
            log.info("Seeded 5 brands");
        }
        if (brandWatchRepository.count() == 0) {
            int[] brandIds = {1, 2, 3, 4, 5, 2, 3, 1, 4, 4};
            for (int i = 0; i < brandIds.length; i++) {
                brandWatchRepository.save(BrandWatch.builder().watchId(i + 1)
                    .brandId(brandIds[i]).updatedAt(LocalDateTime.now()).build());
            }
            log.info("Seeded 10 brand-watch links");
        }
    }

    private WatchBrand brand(String name, String country) {
        return WatchBrand.builder().brandName(name).countryOfOrigin(country).build();
    }
}
