type TagListProps = {
  tags: string[]
  align?: 'center' | 'start'
}

/** The internal classification tags of a local Pokémon. */
export function TagList({ tags, align = 'center' }: TagListProps) {
  return (
    <ul aria-label="Tags" className={`flex flex-wrap gap-1 ${align === 'center' ? 'justify-center' : 'justify-start'}`}>
      {tags.map((tag) => (
        <li key={tag} className="rounded-full border border-slate-300 bg-white px-2 py-0.5 text-xs text-slate-700">
          {tag}
        </li>
      ))}
    </ul>
  )
}
