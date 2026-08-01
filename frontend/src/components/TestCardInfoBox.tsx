import { useState } from 'react';

const TEST_CARDS = [
  {
    label: 'Success',
    number: '4242 4242 4242 4242',
    expiry: '12/30',
    cvv: '123',
  },
  {
    label: 'Insufficient funds',
    number: '4000 0000 0000 9995',
    expiry: '12/30',
    cvv: '123',
  },
  {
    label: 'Invalid card',
    number: '4000 0000 0000 0002',
    expiry: '12/30',
    cvv: '123',
  },
  {
    label: 'Expired card',
    number: '4000 0000 0000 0069',
    expiry: '12/30',
    cvv: '123',
  },
  {
    label: 'Gateway timeout',
    number: '4000 0000 0000 0010',
    expiry: '12/30',
    cvv: '123',
  },
] as const;

const copyText = async (value: string) => {
  try {
    await navigator.clipboard.writeText(value);
    return true;
  } catch {
    return false;
  }
};

const TestCardInfoBox = () => {
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  const handleCopy = async (key: string, value: string) => {
    const ok = await copyText(value);
    if (ok) {
      setCopiedKey(key);
      window.setTimeout(() => setCopiedKey(null), 1500);
    }
  };

  return (
    <div className="rounded-xl border border-sky-500/40 bg-sky-500/10 p-4 space-y-3 text-sm text-sky-50">
      <div>
        <h3 className="font-semibold text-sky-100">Sandbox test cards</h3>
        <p className="text-xs text-sky-200/80 mt-1">
          Click a value to copy. Use any future expiry and a 3-digit CVV. Card data stays in the
          browser — only a scenario token is sent to the API.
        </p>
      </div>

      <div className="space-y-2">
        {TEST_CARDS.map((card) => (
          <div
            key={card.label}
            className="rounded-lg border border-sky-500/30 bg-gray-950/50 p-3 space-y-2"
          >
            <p className="text-xs font-semibold uppercase tracking-wide text-sky-300">
              {card.label}
            </p>
            <div className="grid gap-2 sm:grid-cols-[1.4fr_0.8fr_0.6fr]">
              <button
                type="button"
                onClick={() => handleCopy(`${card.label}-number`, card.number)}
                className="rounded border border-gray-700 bg-gray-900 px-2 py-1.5 text-left font-mono text-xs hover:border-sky-400"
                title="Copy card number"
              >
                {copiedKey === `${card.label}-number` ? 'Copied!' : card.number}
              </button>
              <button
                type="button"
                onClick={() => handleCopy(`${card.label}-expiry`, card.expiry)}
                className="rounded border border-gray-700 bg-gray-900 px-2 py-1.5 text-left font-mono text-xs hover:border-sky-400"
                title="Copy expiry"
              >
                {copiedKey === `${card.label}-expiry` ? 'Copied!' : `Exp ${card.expiry}`}
              </button>
              <button
                type="button"
                onClick={() => handleCopy(`${card.label}-cvv`, card.cvv)}
                className="rounded border border-gray-700 bg-gray-900 px-2 py-1.5 text-left font-mono text-xs hover:border-sky-400"
                title="Copy CVV"
              >
                {copiedKey === `${card.label}-cvv` ? 'Copied!' : `CVV ${card.cvv}`}
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default TestCardInfoBox;
