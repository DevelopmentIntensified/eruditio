# Open content sources for mediaReplacement — sourcing & licensing inventory

*Scope: what we can legally bundle, fetch, or display in-app, given a no-ads monetization model (subscription or one-time purchase). Companion file `research/replace-scrolling.md` covers competitors/science/privacy — not this file.*

**Verdict taxonomy** used on every entry:

- **embed** — we may host/bundle/serve the content inside our app (license permits commercial use + redistribution under our terms).
- **link-only** — deep-link or send the user to the source; never embed/copy.
- **contact-needed** — theoretically possible but requires written permission; treat as link-only until granted.

**The two axis rules that drive most verdicts below**

1. **NC vs. monetization.** Any CC **NC** (NonCommercial) license is a near-certain blocker for a paid app. MIT's own interpretation: "Users may not sell, profit from, or commercialize OCW materials or works derived from them" ([OCW Terms of Use](https://ocw.mit.edu/pages/privacy-and-terms-of-use/)). Whether a *freemium* app counts as "commercial" under CC's definition is a legal gray zone — see [Open questions](#open-questions).
2. **Embedding vs. deep-linking are legally different acts.** Hosting someone's content in your app is redistribution; pointing a link at it is not. Several sources (MIT, Project Gutenberg) explicitly allow linking while restricting embedding.

---

## 1. The MIT family (OpenCourseWare, Open Learning Library, MITx)

| Source | What it offers | License | Commercial use | Verdict |
|---|---|---|---|---|
| [MIT OpenCourseWare](https://ocw.mit.edu/) | 2,500+ full MIT courses: video lectures, notes, problem sets, syllabi | **CC BY-NC-SA 4.0** for the vast majority (some items marked "all rights reserved") | **No** | **link-only** (embed = contact-needed) |
| [MIT Open Learning Library](https://openlearning.mit.edu/courses-programs/open-learning-library) | Interactive self-paced versions of OCW/MITx courses with auto-graded exercises | Varies **per course** — "Some courses will be All Rights Reserved, others Creative Commons, and some have mixed licenses" | Varies | **link-only**; per-course license check before any embed |
| MITx on [edX](https://www.edx.org/school/mitx) | Full course runs, certificates | Per-course, mostly ARR or limited; platform ToS governs | No | **link-only** |

Verified facts:

- OCW's own terms page states the license in full: "Attribution-NonCommercial-ShareAlike 4.0 International (CC BY-NC-SA 4.0)" and includes MIT's interpretation of non-commercial: "Users may not sell, profit from, or commercialize OCW materials or works derived from them" ([Privacy and Terms of Use](https://ocw.mit.edu/pages/privacy-and-terms-of-use/)). A second MIT source confirms: "Non-commercial: Use of MIT OpenCourseWare materials is open to all except for profit-making entities who charge a fee for access to educational…" ([OCW FAQ on Zendesk](https://mitocw.zendesk.com/hc/en-us/articles/4414774353051-What-are-the-requirements-of-use-for-MIT-OpenCourseWare)).
- **Determination is based on the use, not the user**: "Materials may be used by individuals, institutions, governments, corporations… whether for-profit or non-profit so long as the use itself is not a commercialization of the materials" ([OCW Terms](https://ocw.mit.edu/pages/privacy-and-terms-of-use/)). Bundling OCW content into an app the user pays for is selling access to educational material — squarely the example MIT gives ("A commercial education or training business may not offer courses based on OCW materials if students pay a fee for those courses").
- The MIT name/logos may not be used beyond required attribution ([OCW Terms](https://ocw.mit.edu/pages/privacy-and-terms-of-use/)).
- **Deep-linking is unrestricted** — nothing in OCW's terms restricts linking; MIT even instructs website reuse to "include a copy of the license" when embedding ([OCW reuse FAQ](https://mitocw.zendesk.com/hc/en-us/articles/5075793246619-Citation-Attribution-Format-for-OCW-Materials)). The key distinction: pointing users at `ocw.mit.edu` costs us nothing; hosting NC courseware inside a paid product does not.
- **Code is a different story from content.** The [mitodl GitHub org](https://github.com/mitodl) (OCW/MITx platform tooling) publishes code under open licenses — e.g. `mitodl/edx-sga` AGPL-3.0, `mitodl/edx2course_axis` AGPL-3.0, `mitodl/PASSSL` GPL-2.0 (LICENSE data read from each repo's GitHub API metadata). Open-licensed code ≠ open-licensed course content.
- The Open Learning Library says explicitly: download "Only if the material is Creative Commons-licensed, as is the case for all material from MIT OpenCourseWare," and "The courses here will have varying licenses… Some courses will be All Rights Reserved" ([OLL About/FAQ](https://openlearninglibrary.mit.edu/about)).

**Bottom line for NC content in a paid app:** embedding OCW/MITx content in a subscription app would require MIT's permission. **Embedding would be contact-needed at best; treat as link-only.** Note the tension explicitly: the content is "free" but not free-*to-embed-in-a-monetized-app*.

---

## 2. Public-domain classics, books, and wiki corpora

### Project Gutenberg — **embed** (with care)

- **What:** 75,000+ free ebooks, all US-public-domain (or permission-licensed) texts: classics, older non-fiction, PD translations (Jowett's Plato, George Long's Marcus Aurelius, etc.).
- **License:** Per [The Project Gutenberg License](https://www.gutenberg.org/policy/license.html): works "not restricted by U.S. copyright law… can be copied and distributed to anyone in the United States without paying any fees or charges" (§1.E.2). Commercial redistribution **is allowed** — §1.E.8 permits charging "a reasonable fee" for PG works (with a 20% royalty on gross profits **if you keep the "Project Gutenberg" trademark/branding**), and §1.C confirms you may "copy, distribute, perform, display or create derivative works based on the work as long as all references to Project Gutenberg are removed."
- **Practical embedding pattern (per the license page itself):** either (a) keep the PG header/license with the text — free redistribution, no fees charged for the eBook itself; or (b) strip all PG references and treat the underlying text as public domain (allowed for US-PD works; a few works are "posted with permission" and flagged [*]).
- **Trademark royalty:** only if you "use the name Project Gutenberg anywhere in the ebooks you distribute or on the distribution medium or in advertising" and charge money — then 20% of gross profits from those works ([license §1.E.8](https://www.gutenberg.org/policy/license.html); [PG permission how-to](https://www.gutenberg.org/policy/permission.html)). A clean app avoids the name entirely in any book-branded context and pays nothing.
- **Deep-linking rules (binding on us if we link):** link only to landing pages (`gutenberg.org/ebooks/11`), never to raw files; "We do not allow large-scale deep-linking to eBook files hosted on our servers" ([Terms of Use](https://www.gutenberg.org/policy/terms_of_use.html)). Bulk downloads must come from mirrors, not the main site. Note: their ToS also says "If you need to link to a specific file or passage, please make your own copy of that file and host it on your own server" — i.e., **PG itself directs app developers toward bundling copies rather than hotlinking files**.
- **US-only framing:** PG targets US law; texts "may still be copyrighted in other countries" ([Terms of Use](https://www.gutenberg.org/policy/terms_of_use.html)). Fine for a US store launch; flag for international expansion.
- Verdict: **embed** (bundle texts from mirrors; attribute sensibly; don't trade on the name).

### Standard Ebooks — **embed**

- **What:** Volunteer-produced, professionally typeset editions of US-PD literature (epub/azw3/kepub), built in public on GitHub.
- **License:** Each ebook's [LICENSE](https://github.com/standardebooks/arthur-conan-doyle_brigadier-gerard-stories/blob/master/LICENSE.md) states: "The source text and artwork in this ebook are believed to be in the United States public domain… The creators of, and contributors to, this ebook dedicate their contributions to the worldwide public domain via the terms in the CC0 1.0 Universal Public Domain Dedication." Their [About page](https://standardebooks.org/about) says the same: "Standard Ebooks dedicates its own work to the public domain, thus releasing the entirety of each ebook file into the public domain."
- **Bulk download** exists but is gated behind their [Patrons Circle](https://standardebooks.org/bulk-downloads) (a donation perk); individual ebooks and the per-book GitHub repos are unrestricted. Our build pipeline can pull from GitHub at no cost.
- Caveat: "may still be copyrighted in other countries" — same US-first caveat as PG.
- Verdict: **embed** (best-quality PD epub source; CC0 on the production).

### Wikisource, Wikibooks, Wikiversity, Wikipedia, Wiktionary (Wikimedia) — **embed with attribution + share-alike**

- **License:** Wikipedia text (and by the same Wikimedia licensing policy, most WMF project text) is dual-licensed **CC BY-SA 4.0 + GFDL**, with some text CC BY-SA-only: "Most of Wikipedia's text and many of its images are co-licensed under the Creative Commons Attribution-ShareAlike 4.0 International License (CC BY-SA) and the GNU Free Documentation License… Some text has been imported only under CC BY-SA and CC BY-SA-compatible license and cannot be reused under GFDL" ([Wikipedia:Copyrights](https://en.wikipedia.org/wiki/Wikipedia:Copyrights)).
- **Commercial use: allowed.** CC BY-SA has no NC term; the conditions are attribution + share-alike (derived content must carry CC BY-SA). Wikipedia's own interpretation: "Wikipedia content can be copied, modified, and redistributed if and only if the copied version is made available on the same terms to others and acknowledgment of the authors… is included (a link back to the article is generally thought to satisfy the attribution requirement)" ([Wikipedia:Copyrights](https://en.wikipedia.org/wiki/Wikipedia:Copyrights)). The legal code is the binding text: [CC BY-SA 4.0 legalcode](https://creativecommons.org/licenses/by-sa/4.0/legalcode).
- **Attribution in practice for an app:** credit the authors + link to the article; if we *modify* an article, we must indicate changes and license our modified version CC BY-SA. Fair-use-tagged media inside Wikipedia is **not** under the site license — exclude non-free images ([Wikipedia:Copyrights](https://en.wikipedia.org/wiki/Wikipedia:Copyrights), "fair use" section). Image-by-image licensing varies; check each file's description page.
- Verdict: **embed** (with per-page attribution UI and CC BY-SA pass-through on derivatives). Quality for encyclopedic lookup is high; Wikibooks/Wikiversity **language-course quality varies by language** — usable but not uniformly classroom-grade (see §8).

### LibriVox — **embed** (audio)

- **What:** 21,000+ completed volunteer-recorded audiobook projects in 49 languages ("Cataloged works: 21,777", "Non-English works: 2,717", "Number of languages: 49" — [LibriVox homepage](https://librivox.org/)).
- **License/status:** "because all LibriVox recordings are in the Public Domain, third parties – app developers, archivists, streaming channels and curators – are more than welcome to share and re-share them" — official [LibriVox API update, Sept 2026](https://librivox.org/2026/09/16/librivox-api-update/). The **recording** is PD; the **underlying book** must also be checked (usually the same PD text, which is why LibriVox chose it).
- **Bulk/API access:** official API returns up to 500 records/request with a "since" parameter for incremental sync; LibriVox explicitly welcomes app developers, with rate-politeness rules ([API update](https://librivox.org/2026/09/16/librivox-api-update/); [API info](https://librivox.org/api/info)).
- Coverage for our target languages: German, Spanish, Russian have substantial non-English catalogs (2,717 non-English works across 49 languages); Hebrew is thin ([INFERENCE from stats], verify per-title).
- Verdict: **embed**.

### Perseus Digital Library — **link-only**

- **What:** Greek/Latin texts, lexica (LSJ, Lewis & Short), and classical reference tools.
- **License:** Tufts "holds the overall copyright to the Perseus Digital Library; the materials therein (including all texts, translations, images, descriptions, drawings, etc.) are provided for the personal use of students, scholars, and the public. Any commercial use or publication without authorization is strictly proh[ibited]" — [Perseus Copyrights page](https://www.perseus.tufts.edu/hopper/help/copyright) (site returns 503 at time of writing; statement verified from the Internet Archive capture: [web.archive.org snapshot of /hopper/help/copyright](http://web.archive.org/web/2023/https://www.perseus.tufts.edu/hopper/help/copyright)).
- The *underlying ancient texts* are PD, but Perseus's edition apparatus/translations are not. Verdict: **link-only** (commercial embedding requires authorization).

### Public-domain translations of the classics — the trap, and the answer

- **Modern translations are copyrighted.** Penguin Classics, modern classroom translations, and the Loeb Classical Library (Harvard University Press) editions are 20th-century translations owned by their publishers [UNVERIFIED — verify per-edition imprint page; the Perseus copyright page above shows the category of problem].
- **Old translations are public domain.** Benjamin Jowett's Plato translations (19th c.), George Long's *Meditations* (1862), and Elizabethan-era renderings are US-PD and available via Project Gutenberg ([PG terms](https://www.gutenberg.org/policy/license.html): works "not restricted under U.S. copyright law" can be reused freely; thousands of Jowett/Long texts are in the catalog via [PG's index](https://www.gutenberg.org/dirs/GUTINDEX.ALL)).
- Practical rule: for every classic we feature, we ship the PD translation and deep-link (never embed) modern ones. **Quality, not licensing, is the bottleneck** — Jowett's Greek is Victorian English; consider modernizing with our own CC-BY-licensed "friendly retellings" built *on top of* PD texts (derivative works of PD are fully ours).

---

## 3. Open textbooks

| Source | License (verified) | Commercial use | Verdict |
|---|---|---|---|
| [OpenStax](https://openstax.org/) | **CC BY-NC-SA 4.0 across the library** since the [April 2026 licensing update](https://openstax.org/blog/openstax-licensing): "OpenStax has transitioned our textbook library from a mix of CC BY and CC BY-NC-SA licensing to CC BY-NC-SA licensing across the library. This change will apply to all newly released and updated versions of our titles, with limited exceptions." | **No** ("allows OpenStax to thoughtfully consider how commercial entities use our content"; reuse requests go through [their form](https://openstax.org/blog/openstax-licensing)) | **link-only** (embed = contact-needed) |
| [Open Textbook Library](https://open.umn.edu/opentextbooks) (Open Education Network, U. Minnesota) | Aggregator of **1,882** books, each carrying its own author/publisher license — "Open textbooks are licensed by authors and publishers to be freely used and adapted. Download, edit and distribute them at no cost." ([site](https://open.umn.edu/opentextbooks)) | Varies per book — filter for CC BY | **embed (per-book check)** |
| [Saylor University](https://www.saylor.org/LicensingInformation) | "Except where otherwise noted, our course outlines, syllabi, framing notes, and other original content attributed to Saylor University are available under a Creative Commons Attribution license" — but exam banks are excluded: "the questions and answers that comprise our exam 'banks'… are not licensed for reuse and must not be copied, adapted, posted or distributed without permission" ([Licensing Information](https://www.saylor.org/LicensingInformation)) | Yes for CC BY content | **embed (original content only, no exams)** |
| BCcampus OpenEd (BC, Canada) | Typically CC BY, but [UNVERIFIED — site unreachable during research (opencampus.bccampus.ca); verify per-book license before any use] | n/a | **embed (per-book check, unverified)** |

**The NC-vs-monetization tension, stated plainly:** OpenStax is the highest-quality free textbook library in the world, and as of April 2026 it is uniformly NC-licensed ([licensing update](https://openstax.org/blog/openstax-licensing)). A commercial app cannot redistribute it without permission; we can only deep-link. This is the same structural problem as MIT OCW (§1). Anything we *embed* from the textbook world must come from genuinely commercial-permissive shelves (Open Textbook Library CC BY titles, Saylor originals, Wikibooks).

---

## 4. GitHub-hosted open curricula & exercise frameworks

| Source | Content license (verified at source) | Verdict |
|---|---|---|
| [freeCodeCamp](https://github.com/freeCodeCamp/freeCodeCamp) | Repo-wide **BSD 3-Clause** for "freeCodeCamp.org's open-source codebase and curriculum" (LICENSE via GitHub API metadata, repo `freeCodeCamp/freeCodeCamp`) | **embed** (curriculum text + code) |
| [The Odin Project curriculum](https://github.com/TheOdinProject/curriculum) | **CC BY-NC-SA 4.0** — repo's [license.md](https://github.com/TheOdinProject/curriculum/blob/main/license.md): "you can do whatever you want with the material as long as it's for non-commercial use" | **link-only** (embed = contact-needed) |
| [OSSU computer-science](https://github.com/ossu/computer-science) | **MIT** (repo license metadata) — it is a curated path of *links* to other providers (many of them NC), so OSSU's own structure is embeddable; the linked courses keep their own licenses | **embed** (the path/manifest) |
| teachyourselfcs.com | No license statement found on the site (checked homepage HTML) → default © | **link-only** |
| [Exercism](https://github.com/exercism/cpp) exercise content | Track repos carry per-track licenses; e.g. `exercism/cpp` is **MIT** (GitHub API metadata); the site codebase `exercism/website` is **AGPL-3.0** | **embed** (track content, per-track check) |
| Khan Academy **Perseus** exercise framework | **MIT** — repo `Khan/perseus`, "Perseus is Khan Academy's exercise question editor and renderer" (GitHub API metadata). *Code only*: we can use the framework to render/host **our own** exercises. | **embed** (framework) |
| Khan Academy *content* (videos/exercises) | **Not redistributable** — Terms: "The visual interfaces, graphics, design, compilation, information, computer code… educational videos and exercises… and all other elements of the Services (the 'Services Materials') are protected by… intellectual property and proprietary rights… owned by or licensed to Khan Academy" ([Khan Academy Terms of Service §8.1](https://www.khanacademy.org/about/docs/khan-academy-terms-of-service)) | **link-only** |
| [MDN Web Docs](https://github.com/mdn/content) | "All prose content is available under (CC-BY-SA 2.5)" plus per-license code samples ([mdn/content LICENSE.md](https://github.com/mdn/content/blob/main/LICENSE.md)) | **embed** (CC BY-SA conditions) |
| [roadmap.sh](https://roadmap.sh/terms) (developer roadmaps) | Site terms: "These Terms of Use permit you to use the Website for your personal, non-commercial use only. You must not reproduce, distribute, modify, create derivative works of, publicly display, publicly perform, republish, download, store, or transmit any of the material on this Website" ([roadmap.sh Terms of Use](https://roadmap.sh/terms)) | **link-only** |
| Open-source Anki decks (.apkg) | **Per-deck licensing reality:** AnkiWeb shared decks are uploaded by individual authors under whatever terms they state on each deck page; there is no global license. Treat each deck like a blog post: check its stated license, else contact or skip ([apps.ankiweb.net shared decks](https://apps.ankiweb.net/) — pattern note; per-deck verification required) | **embed (per-deck)** |

**Pattern worth naming:** open-source *code* licenses (MIT/BSD/AGPL) let us embed tools and frameworks, but almost never the *content* inside a provider's product. freeCodeCamp and MDN are the big exceptions — genuinely open content at scale.

---

## 5. Papers & scholarly metadata

- **arXiv** ([License info](https://info.arxiv.org/help/license/index.html)): every article "can be viewed and downloaded freely," but **per-paper licenses vary** and are chosen by the submitter: CC BY 4.0, CC BY-SA 4.0, CC BY-NC-SA 4.0, CC BY-NC-ND 4.0, the "arXiv.org perpetual, non-exclusive license 1.0" ("gives limited rights to arXiv to distribute the article, and also **limits re-use of any type from other entities**"), or CC0. The arXiv license and BY-NC-ND/NC-ND variants are **not embeddable** in our app. There is no bulk-download of full text for arbitrary reuse. Metadata is CC0 ("A Creative Commons CC0 1.0 Universal Public Domain Dedication will apply to all metadata"). Verdict: **link-only** for full texts (respect per-paper license on any excerpt); **embed** for metadata.
- **PubMed Central OA Subset** ([openftlist](https://pmc.ncbi.nlm.nih.gov/tools/openftlist/)): "articles and preprints that are made available under Creative Commons or similar licenses"; "License terms vary. Please refer to the license statement in each article"; bulk retrieval is only allowed via the official APIs ("PMC Cloud Service, PMC OAI-PMH Service, E-Utilities and BioC API are the only services that may be used for automated retrieval of PMC content. Systematic retrieval… through any other automated process is prohibited"). Verdict: **embed** for CC-BY subset via the sanctioned APIs; **link-only** otherwise.
- **DOAJ** ([doaj.org/faq](https://doaj.org/faq)): directory metadata is open — "Content on this site is licensed under a Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0) license. Copyrights and related rights for article metadata waived via CC0 1.0 Universal." The *articles* it indexes keep their own licenses. Verdict: **embed** (metadata index); **link-only** for article full text unless CC-BY.
- **Unpaywall** ([unpaywall.org](https://unpaywall.org/)) and **OpenAlex** ([docs.openalex.org](https://docs.openalex.org/)): metadata services that locate OA copies of paywalled papers. OpenAlex describes its dataset as openly licensed [UNVERIFIED at extraction time — confirm CC0 status at docs.openalex.org before shipping]. Unpaywall's data is free for any use with attribution [UNVERIFIED — confirm exact terms at unpaywall.org].
- **The Sci-Hub boundary:** every lawful route above (OA licenses, PMC subset, Unpaywall/OpenAlex-located green/gold OA copies) exists because the rights holder permits it. Sci-Hub-style access is not a gray area — it is mass infringement; **never integrate it**. Deep-linking a publisher's landing page (even of a paywalled paper) is fine; serving the PDF ourselves is not.

---

## 6. Blogs — the default-copyright problem

- **Default: ©.** An RSS feed is a *distribution channel*, not a license. "© 2026" or nothing at all means **full-text aggregation requires permission**. The safe, lawful pattern — what aggregators, `Hacker News`, and every reader app does — is **quoting an excerpt + deep-linking to the original** (fair-use-adjacent quotation with clear attribution and traffic back).
- **Verified examples from named writers:**
  - **Paul Graham** — his [General FAQ](https://www.paulgraham.com/gfaq.html) addresses reuse head-on: "Can I mirror an essay on my site? I'd rather you linked to it instead… **Can I include an essay in my school newspaper? Sure, go ahead. Please include the url of the original.** Can I put a translation into another language online? Sure." Verdict: **link-only** by his stated preference; excerpts/translation with link = explicitly welcome.
  - **Bartosz Ciechanowski** (interactive explainers at [ciechanow.ski](https://ciechanow.ski/)) — footer is default copyright ("Copyright © 2026 Bartosz Ciechanowski"); no open license → **link-only**.
  - **Dan Luu** ([danluu.com](https://danluu.com/)) — no license statement found on the site → default © → **link-only**.
  - **Simon Willison** ([simonwillison.net/about](https://simonwillison.net/about/)) — no content license found → **link-only**.
  - **Martin Fowler** ([martinfowler.com](https://martinfowler.com/)) — site footer is plain "©"; his refactoring articles are © → **link-only**.
  - **Uncle Bob / Robert C. Martin** ([blog.cleancoder.com](https://blog.cleancoder.com/)) — no license found on blog → **link-only**; his books (*Clean Code*) are © of their publishers regardless of his blog.
  - **Kent Beck, Matt Pocock, ThePrimeagen** — no explicit content license found; Pocock's [Total TypeScript free tutorials](https://www.totaltypescript.com/tutorials) are free-to-view but not openly licensed → **link-only** (embed = contact-needed per author).
- **RSS reality:** feeds exist for every serious blog and can power an in-app reader of *headlines + excerpts + links* without permission; full-text re-syndication does not follow from RSS.
- **CC-licensed tech/essay blogs exist but are the minority**; none of the named above carries a CC license [UNVERIFIED beyond those checked]. Any "featured essays" feature must default to excerpt+link.

---

## 7. Government sources — "free and no license" learning material

### The statute: 17 USC §105

> "Copyright protection under this title is not available for any work of the United States Government…" — [17 U.S.C. § 105](https://www.law.cornell.edu/uscode/text/17/105).

House report commentary on §105: "The effect of section 105 is intended to place all works of the United States Government, published or unpublished, in the public domain" (same source). But three traps, all source-verified:

1. **Contractor/grantee works are excluded** — §105 "deliberately avoids making any sort of outright… prohibition against copyright in works prepared under Government contract or grant"; an agency "could determine in each case whether to allow an independent contractor or grantee, to secure copyright" ([§105 notes](https://www.law.cornell.edu/uscode/text/17/105)). The US gov can also *hold* transferred copyrights.
2. **Third-party content is mixed in.** NPS: "Not all information or content on this website has been created or is owned by the National Park Service. Some content is protected by third party rights" ([NPS disclaimer](https://www.nps.gov/aboutus/disclaimer.htm)). NASA: "NASA occasionally uses copyright-protected material of third parties with permission on its website… NASA's use does not convey any rights to others" ([NASA media guidelines](https://www.nasa.gov/nasa-brand-center/images-and-media/)).
3. **Seals/logos are never PD-usable** — NASA: "The NASA Insignia, Logotype, identifiers, and imagery are not in the public domain" ([NASA guidelines](https://www.nasa.gov/nasa-brand-center/images-and-media/)); NPS Arrowhead is a registered trademark protected under 18 U.S.C. §701 ([NPS](https://www.nps.gov/aboutus/disclaimer.htm)). Also never imply endorsement (every agency ToS has a non-endorsement clause).

### US federal classroom-grade sources

| Source | Status (verified) | Classroom-grade? | Verdict |
|---|---|---|---|
| **NASA** educational content/images/video | "generally are not subject to copyright in the United States… You may use this material for educational or informational purposes" ([guidelines](https://www.nasa.gov/nasa-brand-center/images-and-media/)); commercial use allowed if no endorsement implied, no logos on merchandise, no identifiable-person likeness issues | Yes — excellent for astronomy/earth science/engineering (images, NASA+, explainers) | **embed** (follow guidelines; strip insignia; check per-image credits) |
| [MedlinePlus](https://medlineplus.gov/about/using/usingcontent/) (NLM/NIH) | Explicit PD list: health topic pages, medical tests, genetics pages, NLM-watermarked illustrations, videos, tutorials "in both English and Spanish" — "You may reproduce, redistribute, and link freely to non-copyrighted content"; XML data feeds offered. But A.D.A.M. encyclopedia articles, ASHP drug monographs, and unattributed stock images are copyrighted | Yes — best consumer-grade health explainers, EN+ES | **embed** the PD list; **link-only** the encyclopedia/monographs |
| **Smithsonian Open Access** | Content marked CC0 is free "for commercial and noncommercial purposes without fee"; everything else is conditions-apply ([Smithsonian Terms of Use §2](https://www.si.edu/termsofuse)) | Yes (artifacts, science, history imagery) | **embed** CC0-marked items; **link-only** the rest |
| **Library of Congress / National Archives** | US-gov works PD by §105; digitized collections vary item-by-item (donated/contractor material) [item-level check needed] | Archive-grade more than course-grade | **embed** verified-PD items; **link-only** rest |
| **USGS / NOAA / USDA / DOE** | Federal employee works are PD (§105); agency disclaimers call out third-party and contractor content [UNVERIFIED per agency — NOAA/USGS pages unreachable at time of writing; pattern per §105 + agency disclaimers] | Mostly raw data/archive; some educational explainers | **embed** verified items |
| **CIA World Factbook** | Historical flagship of PD reuse — CIA itself described its 5,000+ photos as "copyright-free for anyone to access and use" ([CIA World Factbook farewell story](https://www.cia.gov/stories/story/spotlighting-the-world-factbook-as-we-bid-a-fond-farewell/)). **Important update: the Factbook was sunset (Feb 4, 2026)** — the site is retired; content lives in archives. Prior editions remain PD US-gov works usable from archive copies | Was classroom-grade (country stats/geography) | **embed** (archived PD editions; cite the retirement) |
| FBI / DoD / Army University Press / Naval History & Heritage Command | Federal PD works by statute; declassified/FOIA docs are documents, not curricula; agency seals restricted | Mostly raw archive; military-history series is usable for history content | **embed** verified items; **link-only** everything else |
| **National Park Service** history & nature pages | "material created by the National Park Service… unless otherwise indicated, is generally considered in the public domain" ([NPS disclaimer](https://www.nps.gov/aboutus/disclaimer.htm)) | Yes — history/civics/nature explainers | **embed** (per-page credit check) |

### Non-US governments (one row each)

| Government | License | Commercial OK? | Verdict |
|---|---|---|---|
| UK — [Open Government Licence v3.0](https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/) | "worldwide, royalty-free, perpetual, non-exclusive licence"; "You are free to: copy, publish, distribute… **exploit the Information commercially and non-commercially**"; attribution required ("Contains public sector information licensed under the Open Government Licence v3.0"); excludes "the Royal Arms… military insignia; third party rights… identity documents"; non-endorsement clause | **Yes** | **embed** (GOV.UK guidance/educational material) |
| France — Licence Ouverte / Etalab ([text](https://www.etalab.gouv.fr/licence-ouverte-open-licence/)) | Open license modeled on OGL; attribution + commercial reuse permitted | Yes | **embed** (French-language material) |
| EU bodies — reuse under Commission Decision 2011/833/EU [UNVERIFIED — EUR-Lex not fetched during research; confirm before relying] | Generally OGL-equivalent | Yes (per decision) | **embed (unverified)** |
| Canada — [Open Government Licence – Canada](https://open.canada.ca/en/open-government-licence-canada) | Attribution-only license: "You are free to: Copy… Adapt… [use] for any lawful purpose, commercial or non-commercial"; exclusions for "the names, crests, logos, or other official symbols"; non-endorsement clause (verified text) | Yes | **embed** (federal; provinces vary) |

### The trap: "government" ≠ automatically free

- **US state/local governments may claim copyright.** Wikipedia's policy documents the pattern: "In contrast to the federal government, most state and local governments in the United States retain copyright to their employees' work. Such work is not in the public domain" ([Wikipedia:Copyrights](https://en.wikipedia.org/wiki/Wikipedia:Copyrights)). California is instructive in both directions: its state web-use policy says state-presented information is "considered in the public domain" but simultaneously warns "the State does make use of copyrighted data (e.g., photographs) which may require additional permissions" ([CA.gov Use Policy](https://www.ca.gov/use/)) — i.e., check per-item, and expect state agency sites to mix licensed stock imagery.
- Municipal sites routinely embed commercial stock photos with separate rights; the NPS third-party warning above is representative of the whole category.
- Non-US "Crown copyright" jurisdictions (UK, Canada, Australia, NZ) default to copyright-with-permissions unless an OGL applies — hence the one-row licenses above rather than a blanket "free."

**Best classroom-grade federal picks per subject:** science/engineering → NASA media + explainers, USGS, DOE; health → MedlinePlus EN/ES; history/civics → NPS, National Archives, LOC, Smithsonian Open Access; geography/data → archived World Factbook editions (PD, but sunset — treat as a static dataset).

---

## 8. Open-source language-learning materials (Russian, German, Spanish, Hebrew)

### Wikipedia/Wikimedia as learning material — verified

- License: **CC BY-SA 4.0 (+GFDL dual for most text)**, commercial use allowed with attribution + share-alike — see §2 ([Wikipedia:Copyrights](https://en.wikipedia.org/wiki/Wikipedia:Copyrights)). Attribution in an app = author credit + article link; derivatives stay CC BY-SA.
- **Kiwix** is the offline-bundling precedent: a nonprofit producing open-source offline readers ("We create and support open technologies that bring the world's knowledge Offline via our own open-source software" — [kiwix.org](https://kiwix.org/en/)) and hosting bulk ZIM files of Wikipedia/Wikibooks/Wiktionary in 100+ languages. Legally this rests on the same CC BY-SA license we can use; operationally it proves offline bundling at scale is an accepted reuse of Wikimedia content.
- **Wikibooks / Wikiversity / Wiktionary**: same CC BY-SA licensing as all WMF projects. Language-relevant holdings: Wikibooks' German/Spanish language courses exist and are remixable; Wiktionary supplies dictionaries + inflection data; Wikiversity hosts open courses. **Quality varies sharply by language** — German Wikibooks material is comparatively strong; Russian and especially Hebrew course material is thinner ([INFERENCE based on project size norms — assess per language before promising curriculum depth]).

### US-government language courses — **embed** (PD by statute)

- **FSI (Foreign Service Institute)** courses (German, Spanish, Russian; classic textbook+audio sets) are works of US federal employees → PD under [17 USC §105](https://www.law.cornell.edu/uscode/text/17/105). The official State Department FSI pages were not machine-accessible during research (HTTP 403); complete mirrored copies exist at third-party archives (e.g. [Live Lingua FSI collection](https://www.livelingua.com/fsi/), which returned HTTP 200). Because the *works* are PD, the mirror's identity doesn't change our rights — but verify the state.gov original or an archive.org copy before shipping, and treat 3rd-party mirror "compiled by" claims as non-binding.
- **DLI GLOSS** ([gloss.dliflc.edu](https://gloss.dliflc.edu/)): Defense Language Institute lessons ("Global Language Online Support System"), publicly accessible without login for reading lessons; DoD-authored → PD. Coverage: German/Spanish/Russian present; **Hebrew presence in GLOSS: [UNVERIFIED]** (site is a legacy app; check lesson catalog by language).
- **Peace Corps language materials** (text+audio for many host-country languages): US federal works → PD. The [Peace Corps library](https://www.peacecorps.gov/about/library/) blocks automated fetches (HTTP 403 observed), so per-language verification was not completed; mirrors exist on archive.org. Coverage: Spanish (many countries), Russian (nearby-country materials), German (none — not a PC host language), **Hebrew [UNVERIFIED/likely none]**.
- Verdict: **embed** all three — US-PD, and pre-1970s-era FSI/PC audio is exactly the "PD book + PD recording" case (recordings made by US employees as part of official duties, no separate copyright). **Quality note:** authentic but dated (1960s-era recordings); pedagogy is drill-heavy. Licensing is the strength, freshness the weakness.

### Duolingo's open-sourced course data — **not usable**

- Current [github.com/duolingo](https://github.com/duolingo) org contains only tooling repos (MIT/Apache-2.0 libraries) plus two datasets: `duolingo/duolingo-sharedtask-2020` (shared-task data, **no license** attached) and `duolingo/whosaidit` (NOASSERTION) — verified via GitHub API org listing (2026-09).
- The historically referenced incubator/course-data repositories **do not appear in the org anymore**; no Duolingo course-data repo with a reuse license is discoverable. Verdict: **contact-needed at best; treat as unavailable** [UNVERIFIED historically — the old incubator data's license status was never clearly established and the repos are gone].

### Tatoeba sentence corpus — **embed** (with per-sentence diligence)

- Default license for text sentences: "**Creative Commons Attribution 2.0** France license (CC-BY 2.0 FR)" — attribution only, no NC/SA: "using, reusing, modifying and distributing the sentence is only allowed if the name of the author is cited" ([Tatoeba Terms of Use §6.2](https://tatoeba.org/en/terms_of_use)).
- Audio sentences vary per contributor ("audio phrases… can be contributed under different Creative Commons licenses… audio sentences may be under other licenses than Creative Commons") — filter audio by license before bundling (same source, §6.2/6.5).
- Commercial use: "We are not generally opposed to using our content for commercial purposes. However, this choice depends primarily on contributors" (§6.5) — i.e., CC BY is commercial-OK by default; some audio carries NC. **Sentence quality is contributor-generated** — Tatoeba itself warns it "cannot guarantee" translations (§5.1): quality, not licensing, is the bottleneck; filter by translation ratings/sentence ownership before surfacing.

### Open dictionaries & frequency lists

- **Wiktionary-derived frequency lists** — [hermitdave/FrequencyWords](https://github.com/hermitdave/FrequencyWords) ("Repository for Frequency Word List Generator and processed files") is **MIT**-licensed (GitHub API metadata). Built from OpenSubtitles corpora; covers Russian/German/Spanish/Hebrew. Verdict: **embed**.
- **OpenRussian** ([en.openrussian.org](https://en.openrussian.org/about)) — free database dumps of ~92k Russian words with inflections, translations, frequency list; sources are open (Tatoeba, Wiktionary, Starling) per their About page; **no explicit license statement found on the site** → **contact-needed** before embedding the dump.
- **OpenCorpora** ([opencorpora.org](https://www.opencorpora.org/?doc=0)) — Russian open corpus/morphological dictionary; site returned HTTP 521 during research → license **[UNVERIFIED]**; historically community-licensed — verify before use.
- Hebrew specifics: open lexicographic coverage is the weakest of the four languages; plan on Wiktionary (CC BY-SA) + FrequencyWords as the only verified no-contact-needed base [INFERENCE].

### Language Transfer — **contact-needed** (currently link-only)

- **What:** Free audio "Thinking Method" courses — Complete Spanish, German, Greek, Arabic, Italian, Turkish, French + more; downloads offered straight from the site and app ([FAQ](https://www.languagetransfer.org/faq): "Audio files can now be downloaded from the LT app, and directly from the website").
- **License reality:** The site's claims are about *price*, not copyright: "there are no price tags to access any of the material… Everything is free. Genuinely free" ([About](https://www.languagetransfer.org/about)). **No Creative Commons license, public-domain dedication, or redistribution clause was found anywhere on languagetransfer.org** (checked home, about, FAQ, collaborate, course pages — 2026-09). Free-to-listen ≠ licensed-for-embedding. Verdict: **contact-needed** (the creator is famously open; a permission email is plausible), until then **link-only**.

### Anki shared decks — per-deck pattern

- Shared decks on AnkiWeb are individually licensed by their uploaders (from plain © to CC); there is no blanket license. Pattern: only bundle decks whose author explicitly grants reuse; else **link-only** ([apps.ankiweb.net](https://apps.ankiweb.net/)).

---

## 9. Individual writers & bloggers (coding, science, culture)

Covered in §6 with per-author verification. The decision rule:

- **Default copyright** governs everyone unless their site says otherwise; full-post aggregation requires permission from every author.
- Explicitly permissive verified: **Paul Graham** (mirroring declined, school-newspaper inclusion and translations welcomed with links — [gfaq.html](https://www.paulgraham.com/gfaq.html)).
- No license found → **link-only**: Martin Fowler, Kent Beck, Uncle Bob, Matt Pocock, ThePrimeagen, Dan Luu, Simon Willison, Bartosz Ciechanowski, Kevlin Henney [ Henney's site not checked — [UNVERIFIED] ].
- Books are separate: commercial books (Clean Code, etc.) remain © no matter what the author's blog allows.
- **Quality vs. licensing:** for named living writers, licensing is almost never the bottleneck — *permission-seeking* is. A batch permission email offering attribution + link-backs is a cheap, high-value experiment; a few "yes"es unlock an "essay shelf" that competes on quality.

---

## 10. Excerpts from classics & Christian writers

- **Plato/Socrates:** original Greek is long-PD. **Jowett** (1871) and other pre-1929 translations are US-PD (available via Project Gutenberg); **Loeb Classical Library** and all modern translations are © (Harvard University Press / publishers) [UNVERIFIED at imprint level — check each edition]. Verdict: **embed** PD translations; **link-only** modern ones.
- **The Bible:**
  - **KJV** — US public domain (published 1611); UK nuance: the KJV remains under Crown copyright in the UK with printing rights administered by Royal approbation holders [UNVERIFIED — confirm UK status before UK distribution; US use is unaffected by §105 analysis and the 1611 date].
  - **World English Bible** — "The World English Bible is in the Public Domain. That means that it is not copyrighted… You may copy, publish, proclaim, distribute, redistribute, sell, give away… and use the World English Bible as much as you want" — with the note that "World English Bible" is a **trademark** of eBible.org ([eBible WEB page](https://www.ebible.org/web/)). Verdict: **embed** (modern-English, PD, includes MP3 audio).
  - **NIV/ESV and other modern translations are ©** of their publishers (Biblica, Crossway) — never bundle; at most quote within fair-use norms → **link-only**.
- **Charles Spurgeon** (d. 1892) — all published sermons are US-PD (pre-1929); archival editions abound. Where a modern *archival edition* adds © (e.g., CCEL formatting), prefer the PD scans/plain text. Verdict: **embed**.
- **CCEL** (Christian Classics Ethereal Library) — the distribution layer is copyrighted even when the books aren't: "CCEL.org website and special contents copyright 1993-2020 Harry Plantinga… These books may be used for personal, educational, or non-profit purposes. Contact us for permission to republish CCEL works or to use them commercially" ([CCEL Copyright Policy](https://ccel.org/about/copyright.html)). Verdict: **link-only** for CCEL editions; **embed** the same underlying PD texts taken from the original editions.
- **Pre-1929 PD cluster** — John Wesley (d. 1791), George Whitefield (d. 1770), Jonathan Edwards (d. 1758), Oswald Chambers (d. 1917, *My Utmost* PD in US), Andrew Murray (d. 1917), Hudson Taylor's bio, missionary biographies — all pre-1929 US-PD; Project Gutenberg and archive.org host plain-text editions ([PG index](https://www.gutenberg.org/dirs/GUTINDEX.ALL)). Verdict: **embed**.
- **Modern preachers/writers are © by default** except where they grant open distribution. Verified example: **Desiring God (John Piper)** has a formal permissions page: "You may freely use, share, and reproduce this content for personal or noncommercial (not sell or monetize) uses provided that when publishing textual content online or in hard copy, only quotes and excerpts may be used, not the entire work" — and Piper's books on the site carry a publisher's limited license ([Desiring God permissions](https://www.desiringgod.org/permissions)). Verdict: **contact-needed / link-only** — the NC carve-out again excludes a monetized app's embed even where a ministry is generous.

---

## 11. Audio content (books & podcasts)

### Public-domain audiobooks — **embed**

| Source | Rights situation | Access | Verdict |
|---|---|---|---|
| **LibriVox** | Recordings are public domain; "third parties – app developers, archivists, streaming channels and curators – are more than welcome to share and re-share them" ([official API post](https://librivox.org/2026/09/16/librivox-api-update/)); catalog API with paging/since-sync ([API info](https://librivox.org/api/info)); non-English catalog: 2,717 works, 49 languages ([homepage](https://librivox.org/)) — German/Spanish/Russian present, Hebrew sparse [per-title check] | API + archive.org file hosting | **embed** |
| **LibriVox × translations** | A PD recording may read a PD *translation* (Jowett etc.) — both layers PD. Beware LibriVox recordings of *modern* translations: rare, but check the book's source note per title | per-title | **embed** |
| **Project Gutenberg audio** | A small collection of human-read and machine-read audio; same PG license/trademark regime as texts ([PG license](https://www.gutenberg.org/policy/license.html)) | mostly via mirrors | **embed** (minor volume) |
| **Internet Archive** | Hosts LibriVox/FSI/Gutenberg audio as **public files for download** (not a lending API) — direct file URLs are usable | archive.org | **embed** (PD items) |

### Podcasts — **link-only, with the RSS playback pattern**

- **The norm (pattern, not statute):** every mainstream podcast app (Overcast, Pocket Casts, Apple Podcasts) plays episodes by fetching the **publisher's own RSS feed and enclosure URLs at play time**. No copy is made by the app vendor; no re-hosting, no re-encoding. A formal industry statement of this norm is lacking — it is codified in practice and in the podcasting namespace tools ecosystem (Podcasting 2.0 / podcastindex.org) [UNVERIFIED as *legal* authority; it is a well-established technical/social norm and the low-risk default].
- **Re-hosting or re-encoding episode files is not permitted** unless the show licenses it — that is straightforward copyright (the episode file is © the publisher) + platform norms. In-app behavior: stream from the feed's URL, cache for offline per standard player conventions, never re-serve from our CDN.
- **Explicitly licensed educational audio we can rely on:**
  - **Yale Open Courses** — "Most of the lectures and course material within Open Yale Courses are licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 license. Unless explicitly set forth… third-party content is not covered under the Creative Commons license" ([oyc.yale.edu footer](https://oyc.yale.edu/)) → downloadable lecture audio, but **NC** → **link-only** for a paid app (embed = contact-needed).
  - **MIT OCW audio/video** — CC BY-NC-SA (§1) → **link-only**.
  - Notable *weekly* educational podcasts that are explicitly CC-licensed are **rare**; do not count on this category for content [UNVERIFIED — treat every show as © until its site/feed states otherwise].
- **Learning audio for our four languages:** Language Transfer (§8 — link-only pending contact), FSI/Peace Corps course audio on mirrors (PD → **embed**, dated quality), LibriVox language-learning material (PD-era language primers like old "Colloquial X" books read aloud — per-title; variable), modern podcast courses (© → **link-only**).
- **Ruled out:** Audible/Audiobooks.com (DRM, no redistribution, no meaningful API), **Libby/OverDrive/Hoopla** (library lending; redistribution prohibited; no meaningful API) — **link-only at best** (deep-link into their apps only).

**Audio-specific rights trap:** the *underlying text* and the *recording* have separate rights. LibriVox recording of a PD book = both PD (embed). A modern audiobook of a PD book = © recording (link-only at best). A PD old recording of a © translation = PD recording, © text — don't pair them.

---

## 12. Structured college courses & course platforms (the "syllabus layer")

### Course sources, verified

| Platform | License / ToS status (verified) | Verdict | Notes for a syllabus-layer app |
|---|---|---|---|
| **MIT OCW** (§1) | CC BY-NC-SA 4.0 ([OCW Terms](https://ocw.mit.edu/pages/privacy-and-terms-of-use/)) | **link-only** | Course pages carry syllabi, schedules, unit lists; NC blocks embedding in a paid app. Deep-link always fine. |
| **MIT Open Learning Library** (§1) | Varying per course; "Some courses will be All Rights Reserved, others Creative Commons, and some have mixed licenses" ([OLL FAQ](https://openlearninglibrary.mit.edu/about)) | **link-only** (embed = per-course check, contact for ARR) | Open edX platform; no public API documented; interactive exercises only on their site. |
| **Open Yale Courses** | "Most of the lectures and course material within Open Yale Courses are licensed under a Creative Commons Attribution-Noncommercial-Share Alike 3.0 license. Unless explicitly set forth… third-party content is not covered" ([OYC site footer](https://oyc.yale.edu/)) | **link-only** (embed = contact-needed) | Lecture video/audio + transcripts + materials downloadable; NC + share-alike both block paid-app embedding. |
| **Saylor Academy** | "© Saylor University 2010–2026 except as otherwise noted. **Excluding course final exams, content authored by Saylor University is available under a Creative Commons Attribution 3.0 Unported license.** Third-party materials are the copyright…" ([Saylor Terms of Use](https://www.saylor.org/TermsOfUse); also [Licensing Information](https://www.saylor.org/LicensingInformation)) | **embed** for Saylor-authored CC BY content (attribution, no exams); **link-only** for their 3rd-party-linked material and certificates | CC BY 3.0 = commercial use allowed with attribution — the cleanest genuinely-embeddable full-course library found. Their ToS separately prohibits framing/co-branding the site itself ([ToU §6](https://www.saylor.org/TermsOfUse)) — embed *content*, not a framed site. |
| **OpenLearn (Open University)** | Site-wide default: "Unless specifically stated in a Creative Commons licence… you may not copy, reproduce… or otherwise use Open University content in any way except for your own personal, non-commercial use" ([OU Conditions of use](https://www.open.ac.uk/about/main/management/policies-and-statements/conditions-use-open-university-websites)); individual free courses carry a **CC BY-NC-SA** badge ("The Open University is proud to release this free course under a Creative Commons licence" + by-nc-sa badge, verified on a live course page) with third-party-material carve-outs | **link-only** (NC blocks paid-app embed) | The interesting surface: every OpenLearn course offers machine-readable downloads — **Word, PDF, EPUB 2/3, RSS, HTML, OUXML, IMS Common Cartridge** (verified on [a live course page](https://www.open.edu/openlearn/digital-computing/introduction-cyber-security-stay-safe-online/content-section-overview)) — i.e., structured course scaffolding exists, but the NC licence means we link rather than bundle for a paid app. |
| **edX / Coursera / Canvas-class LMS platforms** | Coursera ToS: "we grant you a limited, personal, non-exclusive, non-transferable, and revocable license right to use our Services… only for your personal, non-commercial use" and prohibits users who "Reproduce, transfer, sell, resell, or otherwise misuse any content" ([Coursera Terms](https://www.coursera.org/about/terms)). edX hosts MITx content under per-course licenses (§1); the [Open edX platform code](https://github.com/edx/edx-platform) is AGPL-3.0 (code, not content) | **link-only** | No embed path; deep links to public course pages are fine. Platform code AGPL is irrelevant to content rights. |

### What a syllabus layer can legitimately build

- **Facts are not protected.** 17 USC §102(b): "In no case does copyright protection for an original work of authorship extend to any idea, procedure, process, system, method of operation, concept, principle, or discovery" ([17 USC §102](https://www.law.cornell.edu/uscode/text/17/102)). A course's *structure* — unit titles, order, topic list, reading list entries — is factual/systematic information, not protectable expression.
- So: **course-level sequencing metadata (unit lists, topic order, "finish X before Y") can be built from public course pages** and used as our own scaffolding — the app shows our own unit plan and deep-links each step to the source. What we must not do is copy the *expression*: course descriptions, lecture text, video, or the site's UI. Where a source's ToS additionally bans automated access (Coursera's anti-circumvention clause), respect it: use humans-and-browser for a handful of flagship courses, or link users straight in.
- Safest implementation: hand-maintained unit maps for the courses we feature (structure facts + our own descriptions), each unit deep-linking to the provider. This is compatible with every platform above.

### NC constraint: embedding lecture video/audio in a monetized app

- NC-licensed lecture material (MIT OCW, Open Yale, OpenLearn) **cannot be bundled or streamed from our servers** inside a paid app — MIT's gloss ("may not sell, profit from, or commercialize… works derived from them", [OCW Terms](https://ocw.mit.edu/pages/privacy-and-terms-of-use/)) and Yale's NC-SA license both bite. **Deep-linking is always permitted** — no license page examined restricts linking to public course pages.
- Practical split: embed = only genuinely commercial-permissive content (Saylor CC BY originals, §4 curricula); everything NC → track progress *around* a link, never around a bundle.

### The four languages — college-grade open material coverage

| Language | PD/CC college-grade course material? |
|---|---|
| German | Yes — FSI German (PD, §8), LibriVox German non-English catalog (§2), plus college-grade open textbooks via Open Textbook Library [per-title check] |
| Spanish | Yes — FSI Spanish (PD), Tatoeba/LibriVox Spanish, Saylor-authored CC BY language content [verify course-by-course] |
| Russian | Moderate — FSI Russian (PD, dated), OpenRussian/FrequencyWords data (§8), LibriVox Russian; college-grade *modern* open courses are thin |
| Hebrew | **Thinnest** — no FSI/Peace Corps Hebrew found ([UNVERIFIED for DLI GLOSS](#)); falls back to Wiktionary/Tatoeba data + PD translations; college-grade PD material essentially absent [INFERENCE — verify before promising a Hebrew track] |

---

## Embedding-vs-linking summary table

| Source | Verdict | Blocking constraint |
|---|---|---|
| Project Gutenberg texts | **embed** | Trademark only; strip branding or keep header |
| Standard Ebooks | **embed** | CC0 — none |
| Wikimedia (Wikipedia/Wikibooks/Wiktionary/Wikisource/Wikiversity) | **embed** | CC BY-SA attribution + share-alike |
| LibriVox audio | **embed** | None (PD) |
| freeCodeCamp curriculum | **embed** | BSD — none |
| MDN | **embed** | CC BY-SA 2.5 |
| Saylor originals | **embed** | CC BY, no exam banks; no framing of their site |
| OpenLearn (Open University) | **link-only** | Site default © / personal non-commercial; free courses CC BY-NC-SA |
| Open Textbook Library (CC BY titles) | **embed** | per-book license |
| Exercism track content | **embed** | per-track (MIT common) |
| Perseus framework code (Khan Perseus) | **embed** | MIT (code only) |
| MIT OCW / MITx / MIT OLL | **link-only** | NC + MIT's noncommercial interpretation |
| OpenStax | **link-only** | CC BY-NC-SA across library (2026) |
| The Odin Project | **link-only** | CC BY-NC-SA |
| Khan Academy content | **link-only** | Proprietary ToS |
| roadmap.sh | **link-only** | Personal non-commercial ToS |
| arXiv full texts | **link-only** | Per-paper licenses |
| Perseus Digital Library | **link-only** | No commercial use w/o authorization |
| Yale Open Courses | **link-only** | CC BY-NC-SA 3.0 |
| Language Transfer | **link-only** → **contact-needed** | Free ≠ licensed |
| OpenRussian dumps | **contact-needed** | No stated license |
| Individual blogs | **link-only** | Default © |
| Desiring God | **contact-needed / link-only** | NC carve-out |
| US federal (NASA, MedlinePlus, NPS, FSI, DLI, Peace Corps, Factbook-archival, Smithsonian CC0) | **embed** | §105 PD; check third-party items; no seals/logos; no implied endorsement |
| UK OGL / Canada OGL / French Licence Ouverte material | **embed** | Attribution + non-endorsement |
| Podcasts (via RSS) | **link-only** (stream at play time) | Re-hosting prohibited |

---

## Open questions

1. **Does a freemium app count as "noncommercial" under CC?** Under CC's definition, NC means "not primarily intended for or directed towards commercial advantage or monetary compensation" ([CC BY-NC-SA legalcode](https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode)). A paid subscription is hard to reconcile with NC; a free app upselling unrelated features is a genuine gray zone. MIT's own gloss (profit from → prohibited) suggests the strict reading. **This is a legal gray zone, not advice — anything NC needs counsel or a license conversation with the licensor.**
2. **PG trademark boundary in practice:** if our paid app ships PG texts with the license header intact but never charges "for the copies" specifically, does the 20% royalty clause (§1.E.8, royalties on "use of Project Gutenberg-tm works") attach? The safest posture is to strip PG branding for bundled texts and use mirrors for bulk downloads, keeping the app's subscription clearly unbundled from the eBook charge.
3. **Which Wikibooks/Wikiversity language courses are actually classroom-grade for German/Spanish/Russian/Hebrew?** Licensing is clean (CC BY-SA); quality is the open question. Requires a content audit, not a legal one.
4. **Hebrew availability:** FSI (none/unclear), DLI GLOSS ([UNVERIFIED]), Peace Corps (likely none), LibriVox Hebrew ([UNVERIFIED]). Hebrew may need to lean on Wiktionary/Tatoeba/FrequencyWords + our own PD-translation classics.
5. **OpenAlex/Unpaywall exact data licenses** — confirm CC0/attribution terms at their docs before building on metadata.
6. **Does streaming podcast audio from a publisher's RSS at play time create any liability beyond the industry norm?** No formal legal authority was found; treat as the established pattern, keep per-episode attribution, and add a takedown channel.
7. **CCEL's "contact us for commercial use"** applies to their *editions*; shipping the same PD texts from PG/archive.org sidesteps it — but formatted study notes, introductions, and cross-references they add stay theirs.
8. **NASA content inside a paid app:** the guidelines' non-commercial framing ("generally are not subject to copyright in the United States… for educational or informational purposes") is agency guidance, not a CC license; commercial use is addressed under separate rules (no endorsement, no merchandise). Embedding NASA imagery in a paid learning app appears permitted under their guidelines but is worth a written confirmation.
9. **Syllabus layer vs. platform ToS:** building course-structure facts is §102(b)-safe ([17 USC §102](https://www.law.cornell.edu/uscode/text/17/102)), but platforms like Coursera pair content © with anti-automation clauses ([Coursera Terms](https://www.coursera.org/about/terms)). Hand-maintained unit maps are the safe path; automated syllabus harvesting is not, without permission.
10. **Hebrew college-grade track (§12):** with no PD/CC college course material found, is a Hebrew track viable on data sources alone (Wiktionary/Tatoeba/FrequencyWords + PD translations), or does it need commissioned CC-licensed content?

---

## Sources (grouped)

**Licenses / statutes (primary)**
- MIT OCW Privacy and Terms of Use (CC BY-NC-SA 4.0 + MIT noncommercial interpretation): https://ocw.mit.edu/pages/privacy-and-terms-of-use/
- MIT OCW reuse/requirements FAQ (Zendesk): https://mitocw.zendesk.com/hc/en-us/articles/4414774353051-What-are-the-requirements-of-use-for-MIT-OpenCourseWare ; https://mitocw.zendesk.com/hc/en-us/articles/5075793246619-Citation-Attribution-Format-for-OCW-Materials
- MIT Open Learning Library About/FAQ: https://openlearninglibrary.mit.edu/about
- mitodl GitHub org (code licenses): https://github.com/mitodl
- Project Gutenberg License: https://www.gutenberg.org/policy/license.html ; Permission how-to: https://www.gutenberg.org/policy/permission.html ; Terms of Use (deep-linking/embedding): https://www.gutenberg.org/policy/terms_of_use.html ; full index: https://www.gutenberg.org/dirs/GUTINDEX.ALL
- Standard Ebooks About + example LICENSE: https://standardebooks.org/about ; https://github.com/standardebooks/arthur-conan-doyle_brigadier-gerard-stories/blob/master/LICENSE.md ; bulk downloads: https://standardebooks.org/bulk-downloads
- Wikimedia licensing: https://en.wikipedia.org/wiki/Wikipedia:Copyrights
- LibriVox: homepage stats https://librivox.org/ ; API update post https://librivox.org/2026/09/16/librivox-api-update/ ; API info https://librivox.org/api/info
- Perseus Copyrights (via Internet Archive snapshot; live site 503): http://web.archive.org/web/2023/https://www.perseus.tufts.edu/hopper/help/copyright
- OpenStax licensing update: https://openstax.org/blog/openstax-licensing
- Open Textbook Library: https://open.umn.edu/opentextbooks
- Saylor Licensing Information: https://www.saylor.org/LicensingInformation ; Saylor Terms of Use (CC BY 3.0 clause, no-framing clause): https://www.saylor.org/TermsOfUse
- Open University Conditions of use (default © / personal non-commercial): https://www.open.ac.uk/about/main/management/policies-and-statements/conditions-use-open-university-websites
- OpenLearn course page (CC BY-NC-SA badge + download formats incl. IMS CC): https://www.open.edu/openlearn/digital-computing/introduction-cyber-security-stay-safe-online/content-section-overview
- Coursera Terms of Use (personal non-commercial license; no content reproduction): https://www.coursera.org/about/terms
- Open edX platform code (AGPL-3.0): https://github.com/edx/edx-platform
- 17 USC §102(b) (facts/ideas not protected): https://www.law.cornell.edu/uscode/text/17/102
- freeCodeCamp repo/license: https://github.com/freeCodeCamp/freeCodeCamp
- The Odin Project curriculum license: https://github.com/TheOdinProject/curriculum/blob/main/license.md
- OSSU: https://github.com/ossu/computer-science ; teachyourselfcs: https://teachyourselfcs.com/
- Exercism: https://github.com/exercism/website (AGPL) ; https://github.com/exercism/cpp (MIT)
- Khan Perseus: https://github.com/Khan/perseus ; Khan ToS §8.1: https://www.khanacademy.org/about/docs/khan-academy-terms-of-service
- MDN content LICENSE.md: https://github.com/mdn/content/blob/main/LICENSE.md
- roadmap.sh Terms: https://roadmap.sh/terms
- arXiv License Information: https://info.arxiv.org/help/license/index.html
- PMC Open Access Subset: https://pmc.ncbi.nlm.nih.gov/tools/openftlist/
- DOAJ footer/FAQ: https://doaj.org/faq
- Unpaywall: https://unpaywall.org/ ; OpenAlex docs: https://docs.openalex.org/
- 17 USC §105: https://www.law.cornell.edu/uscode/text/17/105
- NASA media usage guidelines: https://www.nasa.gov/nasa-brand-center/images-and-media/
- MedlinePlus reuse policy: https://medlineplus.gov/about/using/usingcontent/
- Smithsonian Terms of Use: https://www.si.edu/termsofuse
- NPS disclaimer: https://www.nps.gov/aboutus/disclaimer.htm
- CIA World Factbook farewell: https://www.cia.gov/stories/story/spotlighting-the-world-factbook-as-we-bid-a-fond-farewell/
- UK OGL v3: https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/
- France Licence Ouverte: https://www.etalab.gouv.fr/licence-ouverte-open-licence/
- Canada OGL: https://open.canada.ca/en/open-government-licence-canada
- CA.gov Use Policy: https://www.ca.gov/use/
- Tatoeba Terms of Use: https://tatoeba.org/en/terms_of_use
- Language Transfer: https://www.languagetransfer.org/ (home/about/FAQ/course pages, license absence verified 2026-09)
- DLI GLOSS: https://gloss.dliflc.edu/ ; Live Lingua FSI mirror: https://www.livelingua.com/fsi/
- OpenRussian About: https://en.openrussian.org/about ; OpenCorpora: https://www.opencorpora.org/?doc=0
- hermitdave/FrequencyWords: https://github.com/hermitdave/FrequencyWords
- Duolingo org repos: https://github.com/duolingo
- Kiwix: https://kiwix.org/en/
- World English Bible: https://www.ebible.org/web/
- Desiring God permissions: https://www.desiringgod.org/permissions
- CCEL Copyright Policy: https://ccel.org/about/copyright.html
- Paul Graham General FAQ: https://www.paulgraham.com/gfaq.html ; essays: https://www.paulgraham.com/articles.html
- Ciechanowski: https://ciechanow.ski/ ; Dan Luu: https://danluu.com/ ; Simon Willison: https://simonwillison.net/about/ ; Martin Fowler: https://martinfowler.com/ ; Clean Coder blog: https://blog.cleancoder.com/ ; Total TypeScript: https://www.totaltypescript.com/
- Yale Open Courses (CC BY-NC-SA 3.0 footer statement): https://oyc.yale.edu/
