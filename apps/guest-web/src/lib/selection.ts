export function toggleRoom(selected: number[], roomId: number): number[] {
  return selected.includes(roomId) ? selected.filter((id) => id !== roomId) : [...selected, roomId];
}

/** Display-only: the server prices the stay again at payment and booking time. */
export function selectionTotalPaise(
  rooms: { id: number; pricePerNightPaise: number }[],
  selected: number[],
  nights: number,
): number {
  return rooms
    .filter((room) => selected.includes(room.id))
    .reduce((sum, room) => sum + room.pricePerNightPaise * nights, 0);
}
