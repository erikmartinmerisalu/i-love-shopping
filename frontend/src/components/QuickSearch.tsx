import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchProductSuggestions } from '../api/catalog';
import type { ProductSuggestion } from '../types/catalog';

export default function QuickSearch() {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [suggestions, setSuggestions] = useState<ProductSuggestion[]>([]);
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedQuery(query.trim()), 200);
    return () => window.clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      if (debouncedQuery.length < 2) {
        setSuggestions([]);
        return;
      }
      try {
        const items = await fetchProductSuggestions(debouncedQuery);
        if (!cancelled) {
          setSuggestions(items);
          setActiveIndex(-1);
        }
      } catch {
        if (!cancelled) {
          setSuggestions([]);
        }
      }
    };
    void load();
    return () => {
      cancelled = true;
    };
  }, [debouncedQuery]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const goToSearch = (term: string) => {
    const trimmed = term.trim();
    if (!trimmed) {
      return;
    }
    setOpen(false);
    navigate(`/search?q=${encodeURIComponent(trimmed)}`);
  };

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    if (activeIndex >= 0 && suggestions[activeIndex]) {
      navigate(`/products/${suggestions[activeIndex].id}`);
      setOpen(false);
      return;
    }
    goToSearch(query);
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (!open || suggestions.length === 0) {
      return;
    }
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setActiveIndex((current) => (current + 1) % suggestions.length);
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setActiveIndex((current) => (current <= 0 ? suggestions.length - 1 : current - 1));
    } else if (event.key === 'Enter' && activeIndex >= 0) {
      event.preventDefault();
      navigate(`/products/${suggestions[activeIndex].id}`);
      setOpen(false);
    } else if (event.key === 'Escape') {
      setOpen(false);
    }
  };

  return (
    <div ref={containerRef} className="relative w-full min-w-0">
      <form onSubmit={handleSubmit} role="search" aria-label="Product search">
        <label htmlFor="quick-search" className="sr-only">
          Search products
        </label>
        <input
          id="quick-search"
          type="search"
          value={query}
          onChange={(event) => {
            setQuery(event.target.value);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={handleKeyDown}
          placeholder="Search lamps, bulbs, brands…"
          autoComplete="off"
          className="w-full rounded-lg border border-gray-700 bg-gray-950 px-4 py-2.5 text-sm text-white placeholder-gray-500 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
        />
      </form>

      {open && debouncedQuery.length >= 2 && (
        <ul
          className="absolute z-50 mt-1 w-full rounded-lg border border-gray-700 bg-gray-900 shadow-xl overflow-hidden"
          role="listbox"
          aria-label="Search suggestions"
        >
          {suggestions.length === 0 ? (
            <li className="px-4 py-3 text-sm text-gray-400">No matches — press Enter to search all</li>
          ) : (
            suggestions.map((item, index) => (
              <li key={item.id} role="option" aria-selected={index === activeIndex}>
                <button
                  type="button"
                  className={`flex w-full items-center justify-between gap-3 px-4 py-3 text-left text-sm hover:bg-gray-800 ${
                    index === activeIndex ? 'bg-gray-800' : ''
                  }`}
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => {
                    navigate(`/products/${item.id}`);
                    setOpen(false);
                  }}
                >
                  <span className="truncate">{item.name}</span>
                  <span className="shrink-0 text-primary font-semibold">€{item.price.toFixed(2)}</span>
                </button>
              </li>
            ))
          )}
          <li>
            <button
              type="button"
              className="w-full border-t border-gray-800 px-4 py-2 text-left text-xs text-sky-300 hover:bg-gray-800"
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => goToSearch(query)}
            >
              View all results for “{query.trim()}”
            </button>
          </li>
        </ul>
      )}
    </div>
  );
}
