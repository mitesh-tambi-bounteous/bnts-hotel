# Guest Web — UI guidelines

## Navigation and information architecture
- No sidebar and no main navigation: the real app layout is a bare page with a single centred `<main>`, so the shell has only a brand header ("Guest Web"), the content area and a footer. Add a nav only when a second top-level section actually exists in `layout.tsx`.
- Every screen is one linear column inside the content area, because guests use the app for short, single-purpose tasks.
- Go back with a `.btn-ghost` link at the top of the screen, since there is no nav to return through.

## Layout and density
- Spacious consumer layout: content is capped at `--size-content` and padded with `--space-5`, matching the existing centred `max-w-2xl` page.
- Design mobile-first at 375px; the single column only gains side margins on wider screens, so no other breakpoint is needed.
- Separate sections with `.stack-lg`, and group related controls with `.stack` or `.cluster`, so spacing always comes from the spacing tokens.

## Component usage
- One `.btn-primary` per screen for the main action; `.btn-secondary` for alternatives and `.btn-ghost` for low-emphasis actions, so the next step is obvious.
- `.btn-danger` only for irreversible actions (cancel booking), always behind a `.modal` confirmation, to prevent accidental loss.
- Use `.card` for a self-contained item (a room, a reservation); use a plain `.stack` list for simple sequences; avoid tables, as wide data breaks at 375px.
- Forms: wrap every control in `.field` with a visible label, and show errors with `.is-invalid` plus `.field-error`, so the problem is next to its cause.
- Use `.alert` for page-level messages, `.toast` for confirmations of completed actions, and `.empty-state` when a list has nothing to show.
- Use `.skeleton` while content loads and `.spinner` only inside a button awaiting a response, to keep layout stable.
- Use `.drawer` for secondary detail and `.modal` only for confirmations, because modals interrupt the guest.

## Theme
- Light only (`--color-bg` on `--color-fg`), matching the current app; there is no dark theme until the app ships one, so we don't design states we can't test.
- `--color-primary` is reserved for actions and selection; status uses the success/warning/danger colours, so colour keeps one meaning.

## Voice and tone
- Warm, brief and direct, like a front-desk host: "Your room is ready", not "Reservation status updated".
- Address the guest as "you"; sentence case everywhere; buttons start with a verb ("Book room", "Cancel stay").
- Errors say what to do next, never who is at fault: "Enter a check-in date" rather than "Invalid input".
