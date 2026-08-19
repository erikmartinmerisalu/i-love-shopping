import { useEffect } from 'react';
import { SITE } from '../config/site';

type PageMetaProps = {
  title: string;
  description?: string;
};

const truncate = (value: string, max: number) =>
  value.length <= max ? value : `${value.slice(0, max - 1).trim()}…`;

export default function PageMeta({ title, description }: PageMetaProps) {
  useEffect(() => {
    const fullTitle = truncate(title.includes(SITE.name) ? title : `${title} — ${SITE.name}`, 60);
    document.title = fullTitle;

    let meta = document.querySelector('meta[name="description"]');
    if (!meta) {
      meta = document.createElement('meta');
      meta.setAttribute('name', 'description');
      document.head.appendChild(meta);
    }
    meta.setAttribute('content', description ?? SITE.description);
  }, [title, description]);

  return null;
}
