import { Link } from 'react-router-dom';
import PageMeta from '../components/PageMeta';
import { SITE, TEAM } from '../config/site';

export default function AboutPage() {
  return (
    <>
      <PageMeta
        title="About ESTValgus"
        description="Demo project overview — fictional team and contact details except the listed CEO."
      />

      <div className="page-container py-12">
        {SITE.isDemoSite && (
          <p className="mb-8 max-w-3xl rounded-lg border border-amber-600/40 bg-amber-950/30 px-4 py-3 text-sm text-amber-100">
            Demo site for coursework. Company details, teammates (except the CEO below), addresses, and social links
            are placeholders — not a real business.
          </p>
        )}

        <header>
          <h1 className="text-4xl font-bold">About {SITE.name}</h1>
          <p className="mt-4 max-w-4xl text-lg text-gray-300 leading-relaxed">
            A mock B2C lighting shop built to demonstrate catalog browsing, cart, checkout, and order flows. Product
            data and branding are sample content for evaluation only.
          </p>
        </header>

        <section className="mt-12" aria-labelledby="mission-heading">
          <h2 id="mission-heading" className="text-2xl font-bold">Project mission</h2>
          <p className="mt-4 max-w-4xl text-gray-300 leading-relaxed">
            Show a complete e-commerce experience: searchable catalog, responsive storefront, guest checkout, sandbox
            payments, and admin-ready architecture — without presenting fictional contact details as real.
          </p>
        </section>

        <section className="mt-12" aria-labelledby="team-heading">
          <h2 id="team-heading" className="text-2xl font-bold">Meet the team</h2>
          <p className="mt-2 text-sm text-gray-400">
            One real name (project author). Other cards are mock roles for layout and copy only.
          </p>
          <ul className="mt-6 grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-3 2xl:grid-cols-3">
            {TEAM.map((member) => (
              <li key={member.name} className="rounded-xl border border-gray-800 bg-gray-900 p-6">
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="text-lg font-semibold">{member.name}</h3>
                  {!member.isReal && (
                    <span className="rounded bg-gray-800 px-2 py-0.5 text-xs text-gray-400">Mock</span>
                  )}
                </div>
                <p className="text-sm text-sky-300">{member.role}</p>
                <p className="mt-3 text-sm text-gray-400 leading-relaxed">{member.bio}</p>
              </li>
            ))}
          </ul>
        </section>

        <section className="mt-12 rounded-xl border border-gray-800 bg-gray-900 p-6" aria-labelledby="connect-heading">
          <h2 id="connect-heading" className="text-xl font-bold">Connect with us (demo)</h2>
          <p className="mt-2 text-sm text-gray-400">{SITE.contact.address}</p>
          <p className="mt-1 text-sm text-gray-500">{SITE.contact.email}</p>
          <ul className="mt-4 flex flex-wrap gap-4 text-sm">
            <li>
              <span className="text-gray-500">Instagram (placeholder)</span>
            </li>
            <li>
              <span className="text-gray-500">Facebook (placeholder)</span>
            </li>
            <li>
              <span className="text-gray-500">LinkedIn (placeholder)</span>
            </li>
          </ul>
          <Link
            to="/contact"
            className="mt-6 inline-block rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-focus"
          >
            Demo contact form
          </Link>
        </section>
      </div>
    </>
  );
}
