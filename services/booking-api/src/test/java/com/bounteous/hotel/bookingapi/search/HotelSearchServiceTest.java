package com.bounteous.hotel.bookingapi.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bounteous.hotel.bookingapi.booking.ValidationException;
import com.bounteous.hotel.bookingapi.catalogue.Tab;
import com.bounteous.hotel.bookingapi.search.HotelDetail.RoomView;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class HotelSearchServiceTest {

    private static final LocalDate IN = LocalDate.of(2026, 12, 12);
    private static final LocalDate OUT = LocalDate.of(2026, 12, 14);

    private final HotelSearchService service = new HotelSearchService(new FakeCatalogue());

    @Test
    void listsTheBrandsHotelsInTheCity() { // AC1
        var result = service.search(new SearchQuery("Mumbai", IN, OUT, 4, 2));

        assertThat(result).extracting(HotelSummary::name)
                .containsExactlyInAnyOrder("Harbour View Mumbai", "Bandra Court", "Airport Inn Andheri");
        assertThat(service.search(new SearchQuery("Goa", IN, OUT, 2, 1))).isEmpty();
    }

    @Test
    void showsThePricePerNightInPaiseTaxesIncluded() { // AC2
        var result = service.search(new SearchQuery("Mumbai", IN, OUT, 4, 2));

        assertThat(result.get(0).name()).isEqualTo("Harbour View Mumbai");
        assertThat(result.get(0).fromPricePerNightPaise()).isEqualTo(780_000L);
    }

    @Test
    void doesNotListAHotelWhoseRoomsCannotCoverTheSearchedRoomsAndGuests() { // AC20
        var result = service.search(new SearchQuery("Mumbai", IN, OUT, 9, 3));

        assertThat(result).extracting(HotelSummary::name)
                .contains("Harbour View Mumbai", "Bandra Court")
                .doesNotContain("Airport Inn Andheri", "Marine Drive Residency");
    }

    @Test
    void marksASoldOutRoomAndGivesItNoSelectableStatus() { // AC3, AC4
        var detail = service.detail(FakeCatalogue.HARBOUR, IN, OUT, 4, 2);

        assertThat(room(detail, "Harbour suite").status()).isEqualTo(RoomStatus.SOLD_OUT);
        assertThat(room(detail, "Harbour suite").reason()).isEqualTo("No rooms left for your dates.");
        assertThat(room(detail, "Executive room").status()).isEqualTo(RoomStatus.AVAILABLE);
    }

    @Test
    void marksARoomThatDoesNotSuitTheSearchAsUnsuitable() { // AC21, AC22
        var detail = service.detail(FakeCatalogue.HARBOUR, IN, OUT, 4, 2);

        assertThat(room(detail, "Compact single room").status()).isEqualTo(RoomStatus.UNSUITABLE);
        assertThat(room(detail, "Compact single room").reason())
                .isEqualTo("Doesn't suit your search: sleeps 1, and you're searching 4 guests in 2 rooms.");
    }

    @Test
    void showsOnlyTheTabsTheCatalogueDefinesForTheHotel() { // AC23
        var detail = service.detail(FakeCatalogue.HARBOUR, IN, OUT, 4, 2);

        assertThat(detail.tabs()).extracting(Tab::name).containsExactly("Overview", "Amenities", "Location");
        assertThat(service.detail(FakeCatalogue.AIRPORT, IN, OUT, 2, 1).tabs())
                .extracting(Tab::name)
                .containsExactly("Overview");
    }

    @Test
    void rejectsACheckOutThatIsNotAfterCheckIn() {
        assertThatThrownBy(() -> new SearchQuery("Mumbai", OUT, IN, 2, 1)).isInstanceOf(ValidationException.class);
    }

    private static RoomView room(HotelDetail detail, String name) {
        return detail.rooms().stream().filter(r -> r.name().equals(name)).findFirst().orElseThrow();
    }
}
