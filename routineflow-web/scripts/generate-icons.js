import sharp from 'sharp'
import { mkdirSync } from 'fs'

mkdirSync('public/icons', { recursive: true })

// SVG base — dark background with blue check circle and routine lines
const svgIcon = `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">
  <rect width="512" height="512" rx="80" fill="#0a0a0a"/>
  <rect x="60" y="60" width="392" height="392" rx="60" fill="#141414"/>
  <circle cx="256" cy="200" r="60" fill="none" stroke="#0071e3" stroke-width="20"/>
  <path d="M220 200 L248 228 L300 168" fill="none"
        stroke="#0071e3" stroke-width="20" stroke-linecap="round"
        stroke-linejoin="round"/>
  <rect x="160" y="300" width="80" height="12" rx="6" fill="#0071e3"/>
  <rect x="160" y="328" width="192" height="12" rx="6" fill="#1f1f1f"/>
  <rect x="160" y="356" width="140" height="12" rx="6" fill="#1f1f1f"/>
</svg>`

const svgBuffer = Buffer.from(svgIcon)

await sharp(svgBuffer).resize(192, 192).png().toFile('public/icons/icon-192.png')
console.log('✓ public/icons/icon-192.png')

await sharp(svgBuffer).resize(512, 512).png().toFile('public/icons/icon-512.png')
console.log('✓ public/icons/icon-512.png')

await sharp(svgBuffer).resize(180, 180).png().toFile('public/apple-touch-icon.png')
console.log('✓ public/apple-touch-icon.png')

console.log('Icons generated successfully')
