document.addEventListener('DOMContentLoaded', () => {
    
    // ============================================
    // 1. 极其惊艳的打字机与字符独立爆破动效 (Hero Title)
    // 根据节点类型，深度遍历 h1 词组并在外面包扎带延时 Span
    // ============================================
    const title = document.querySelector('.hero-title');
    if (title) {
        const textNodes = Array.from(title.childNodes);
        title.innerHTML = ''; // 清空原本节点进行重构
        let delayCount = 0;
        
        textNodes.forEach(node => {
            if (node.nodeType === 3) { 
                // 纯文本抓捕
                const words = node.textContent.split('');
                words.forEach(char => {
                    const span = document.createElement('span');
                    span.textContent = char;
                    // 使字符呈现出排山倒海一样的阶梯上移
                    span.style.animationDelay = `${delayCount * 0.05}s`;
                    title.appendChild(span);
                    delayCount++;
                });
            } else if (node.nodeType === 1 && node.tagName.toLowerCase() === 'br') {
                // 原封不动推入换行符
                title.appendChild(document.createElement('br'));
            } else if (node.nodeType === 1) { 
                // 特殊的高亮色节点 span (如 gradient-text)
                const words = node.textContent.split('');
                const outerSpan = document.createElement('span');
                outerSpan.className = node.className;
                words.forEach(char => {
                    const innerSpan = document.createElement('span');
                    innerSpan.textContent = char;
                    innerSpan.style.animationDelay = `${delayCount * 0.05}s`;
                    outerSpan.appendChild(innerSpan);
                    delayCount++;
                });
                title.appendChild(outerSpan);
            }
        });
    }

    // ============================================
    // 2. 超级交叉观察器：掌控长屏各个板块进入视野的三维切入
    // ============================================
    const observerOptions = { 
        root: null, 
        rootMargin: '0px', 
        threshold: 0.15  // 露出15%才掀开头盖骨执行，使得用户能真切看到动画发生
    };
    
    const scrollObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
                observer.unobserve(entry.target); // 一次性释放，节约渲染大盘算力
            }
        });
    }, observerOptions);

    // 绑定各类出场兵种
    document.querySelectorAll('.fade-up, .fade-in-left, .fade-in-right, .fade-in-up').forEach(el => {
        scrollObserver.observe(el);
    });

    // ============================================
    // 3. 压轴科技感底蕴：基于鼠标位置偏移的 3D 浮雕交互 (Parallax Hover)
    // 在科室大屏卡片上实现 具有景深(Perspective) 透视的悬停效果
    // ============================================
    const cards = document.querySelectorAll('.parallax-card');
    cards.forEach(card => {
        // 利用边界矩形框锁定 X Y 轴鼠标定位
        card.addEventListener('mousemove', e => {
            const rect = card.getBoundingClientRect();
            const x = e.clientX - rect.left; 
            const y = e.clientY - rect.top;  
            
            // 求算中轴线心点
            const centerX = rect.width / 2;
            const centerY = rect.height / 2;
            
            // X、Y的倾角换位推算最大偏转度角 (5deg)，实现跟手阻尼
            const rotateX = ((y - centerY) / centerY) * -5;
            const rotateY = ((x - centerX) / centerX) * 5; 
            
            card.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale3d(1.02, 1.02, 1.02)`;
        });
        
        // 鼠标移出后弹性回弹
        card.addEventListener('mouseleave', () => {
            card.style.transform = `perspective(1000px) rotateX(0) rotateY(0) scale3d(1, 1, 1)`;
        });
    });
});
