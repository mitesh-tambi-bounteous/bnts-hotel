import { SearchForm } from "@/components/search-form";
import { SearchResults } from "@/components/search-results";
import { ApiError, searchHotels, type HotelSummary } from "@/lib/api";
import { parseStay, validateStay, type SearchParams, type Stay } from "@/lib/stay";

function addDays(iso: string, days: number): string {
  const date = new Date(`${iso}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

export default async function SearchPage({ searchParams }: { searchParams: Promise<SearchParams> }) {
  const params = await searchParams;
  const today = new Date().toISOString().slice(0, 10);
  const submitted = parseStay(params);
  const stay: Stay = submitted ?? {
    city: "Mumbai",
    checkIn: addDays(today, 14),
    checkOut: addDays(today, 16),
    guests: 2,
    rooms: 1,
  };
  const errors = submitted ? validateStay(submitted, today) : {};

  let hotels: HotelSummary[] | null = null;
  let failed = false;
  if (submitted && Object.keys(errors).length === 0) {
    try {
      hotels = await searchHotels(submitted);
    } catch (error) {
      if (!(error instanceof ApiError)) throw error;
      failed = true;
    }
  }

  return (
    <div className="stack-lg">
      <h1>Find your stay</h1>
      <SearchForm stay={stay} errors={errors} />
      {failed ? (
        <div className="alert alert-danger" role="alert">
          <span className="alert-title">We couldn&apos;t load hotels</span>
          <span>Try your search again in a moment.</span>
        </div>
      ) : null}
      {hotels ? <SearchResults stay={stay} hotels={hotels} /> : null}
    </div>
  );
}
