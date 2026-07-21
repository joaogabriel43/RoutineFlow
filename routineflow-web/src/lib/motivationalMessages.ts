export function getMotivationalMessage(rate: number): string {
  if (rate === 1) {
    const perfect = [
      'Dia perfeito! Você dominou sua rotina hoje. 💎',
      '100% concluído! Consistência é a chave. 🔥',
      'Incrível! Todas as metas do dia foram alcançadas. 🏆',
      'Você é imparável! Missão cumprida. ⭐',
    ]
    return perfect[Math.floor(Math.random() * perfect.length)]
  }

  if (rate >= 0.8) {
    const almost = [
      'Quase lá! Faltam apenas alguns detalhes. 💪',
      'Ótimo progresso hoje! Continue assim. 🚀',
      'A um passo da perfeição!',
    ]
    return almost[Math.floor(Math.random() * almost.length)]
  }

  if (rate >= 0.5) {
    const half = [
      'Metade do caminho andado! Você consegue. 🏃',
      'Bom trabalho até agora. Vamos fechar o dia forte!',
      'Progresso sólido. Cada pequena vitória conta.',
    ]
    return half[Math.floor(Math.random() * half.length)]
  }

  if (rate > 0) {
    const started = [
      'Um passo de cada vez. O importante é começar. 🌱',
      'Você já começou, isso é o mais difícil!',
      'Pequenos progressos ainda são progressos.',
    ]
    return started[Math.floor(Math.random() * started.length)]
  }

  const none = [
    'Pronto para começar o dia? 🌅',
    'O que vamos conquistar hoje?',
    'Um novo dia, uma nova chance de focar no que importa.',
  ]
  return none[Math.floor(Math.random() * none.length)]
}
