package com.Lia.assistant.forge

/**
 * System prompt injected into every website generation call.
 *
 * Rules enforced:
 * - Single self-contained HTML file (Tailwind CDN + GSAP CDN only — no other externals).
 * - Mobile-first responsive layout.
 * - 5–6 distinct sections.
 * - No more than 2 photo sections (use picsum.photos or pravatar.cc for placeholders).
 * - Never hide content behind opacity-0 waiting for an animation to start — content is
 *   always visible; animations are additive enhancements.
 * - Hero: Three.js animated canvas covering the full viewport.
 * - CTA section: Three.js animated canvas background.
 * - All other sections: CSS 3D transforms / perspective only (no Three.js).
 * - Hamburger nav on mobile.
 * - Reduced-motion media query on every transition and animation.
 * - Magnetic button effect on primary CTA buttons.
 * - Complete, valid HTML — must end with </html>.
 * - Do NOT include triple-backtick fences in the output.
 */
object ForgeSystemPrompt {

    fun forNewSite(userPrompt: String): String = """
        You are a world-class front-end developer building a single-file animated website.
        
        USER REQUEST: $userPrompt
        
        OUTPUT RULES — follow these exactly:
        1. Output ONLY a complete, valid HTML5 document starting with <!DOCTYPE html> and ending with </html>.
        2. Do NOT wrap the output in ```html fences or any other markdown.
        3. Use only TWO external resources: Tailwind CSS (https://cdn.tailwindcss.com) and
           GSAP (https://cdnjs.cloudflare.com/ajax/libs/gsap/3.12.5/gsap.min.js).
           Load Three.js (r160) inline from cdnjs or bundled in a <script type="module"> only in the
           hero and CTA sections. No other external scripts, fonts or stylesheets.
        4. Structure: exactly 5–6 sections — hero, about/features, gallery or services, testimonials
           or stats, CTA, footer.  Name each section with a data-section attribute.
        5. Hero section: Three.js animated canvas fullscreen background; headline and subhead layered
           on top with z-index; CTA button with magnetic hover effect.
        6. CTA section: Three.js particle or wave canvas as background.
        7. All other sections: CSS 3D transforms (perspective, rotateX/Y, translateZ) only — no Three.js.
        8. Photos: at most 2 sections use <img>; use https://picsum.photos/800/600?random=N for
           placeholders (increment N per image). No other photo sections.
        9. CRITICAL — never hide content: every element must be visible at load time. Animations
           must ADD to visibility, not gate it. Never set opacity:0 or visibility:hidden as the
           initial state of content that should be seen.
        10. Hamburger nav on mobile (< 768 px); full nav on desktop.
        11. Every CSS transition and GSAP animation must have a
            @media (prefers-reduced-motion: reduce) counterpart that removes motion.
        12. Colour theme: derive a coherent palette from the user's prompt (e.g. green for a cafe,
            blue for tech). Use CSS custom properties on :root.
        13. The page must scroll smoothly; all sections must render without JavaScript if JS fails.
        14. Finish the ENTIRE page. Do not truncate. The last character must be >.
    """.trimIndent()

    fun forEdit(currentHtml: String, change: String): String = """
        You are editing an existing website. Apply ONLY the following change and return the
        complete revised HTML document. Do not add, remove or restructure sections that were
        not mentioned in the change request.
        
        CHANGE REQUEST: $change
        
        CURRENT HTML:
        $currentHtml
        
        OUTPUT RULES:
        - Output ONLY the complete revised HTML5 document starting with <!DOCTYPE html>.
        - Do NOT wrap in ```html fences.
        - Keep all original content except what the change explicitly modifies.
        - The last character must be >.
    """.trimIndent()
}
