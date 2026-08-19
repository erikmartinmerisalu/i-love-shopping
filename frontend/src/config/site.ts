export const SITE = {
  name: 'ESTValgus',
  tagline: 'Modern lighting for every space',
  description:
    'Demo B2C lighting shop for coursework — smart bulbs, desk lamps, pendants, and outdoor lighting with sandbox checkout.',
  /** Shown on About / Contact — clearly fictional, not a real business. */
  isDemoSite: true,
  contact: {
    email: 'support@demo.estvalgus.example',
    phone: '+000 000 0000 (demo)',
    address: 'Unit 0, Demo Commerce Park, Fictional City, XX-0000',
    hours: 'Mon–Fri · demo hours only · no walk-ins',
  },
  social: {
    instagram: 'https://example.com/estvalgus-demo-instagram',
    facebook: 'https://example.com/estvalgus-demo-facebook',
    linkedin: 'https://example.com/estvalgus-demo-linkedin',
  },
} as const;

export const TEAM = [
  {
    name: 'Erik-Martin Merisalu',
    role: 'Founder & CEO',
    bio: 'Project lead for this demo storefront — catalog, checkout flow, and platform requirements.',
    isReal: true,
  },
  {
    name: 'Alex Catalogbot',
    role: 'Head of Product (mock)',
    bio: 'Fictional role for the demo UI. Represents catalog curation and sample product data in the shop.',
    isReal: false,
  },
  {
    name: 'Sam Supportbot',
    role: 'Customer Experience (mock)',
    bio: 'Fictional role for the demo UI. Contact form messages go to a sandbox inbox, not a real team.',
    isReal: false,
  },
] as const;

export function estimateDeliveryDate(from: Date = new Date()): Date {
  const result = new Date(from);
  let added = 0;
  while (added < 5) {
    result.setDate(result.getDate() + 1);
    const day = result.getDay();
    if (day !== 0 && day !== 6) {
      added += 1;
    }
  }
  return result;
}

export function formatDeliveryDate(date: Date): string {
  return date.toLocaleDateString('en-GB', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });
}
