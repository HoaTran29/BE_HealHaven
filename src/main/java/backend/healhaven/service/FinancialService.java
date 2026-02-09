package backend.healhaven.service;

import backend.healhaven.dto.response.FinancialStatsResponse;
import backend.healhaven.entity.User;
import backend.healhaven.repository.BookingRepository;
import backend.healhaven.repository.UserRepository;
import backend.healhaven.repository.VenueBookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialService {

    private final BookingRepository bookingRepository;
    private final VenueBookingRepository venueBookingRepository;
    private final UserRepository userRepository;

    public FinancialStatsResponse getHostFinancialStats(Integer hostId) {
        BigDecimal totalRevenue = bookingRepository.calculateTotalRevenue(hostId);
        BigDecimal totalExpense = venueBookingRepository.calculateTotalExpense(hostId);

        if (totalRevenue == null)
            totalRevenue = BigDecimal.ZERO;
        if (totalExpense == null)
            totalExpense = BigDecimal.ZERO;

        BigDecimal netProfit = totalRevenue.subtract(totalExpense);

        return FinancialStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalExpense(totalExpense)
                .netProfit(netProfit)
                .build();
    }
}
