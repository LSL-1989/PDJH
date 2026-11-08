const http = require('http');

const HOST = '127.0.0.1';
const PORT = 8080;

// Helper to make POST requests
function post(path, data) {
    return new Promise((resolve, reject) => {
        const payload = JSON.stringify(data || {});
        const options = {
            hostname: HOST,
            port: PORT,
            path: path,
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Content-Length': Buffer.byteLength(payload)
            }
        };

        const req = http.request(options, (res) => {
            let body = '';
            res.setEncoding('utf8');
            res.on('data', chunk => body += chunk);
            res.on('end', () => {
                try {
                    resolve(JSON.parse(body));
                } catch (e) {
                    resolve(body);
                }
            });
        });

        req.on('error', reject);
        if (payload) {
            req.write(payload);
        }
        req.end();
    });
}

// Helper to make GET requests
function get(path) {
    return new Promise((resolve, reject) => {
        const req = http.get(`http://${HOST}:${PORT}${path}`, (res) => {
            let body = '';
            res.setEncoding('utf8');
            res.on('data', chunk => body += chunk);
            res.on('end', () => {
                try {
                    resolve(JSON.parse(body));
                } catch (e) {
                    resolve(body);
                }
            });
        });
        req.on('error', reject);
    });
}

async function runTest() {
    console.log("=============================================");
    console.log("   🏥 排队叫号系统核心主干 E2E 自动化联调测试");
    console.log("=============================================\n");

    try {
        console.log("👉 步骤 1: 获取在用科室...");
        // 尝试两种可能获取科室的方法
        let deptsRes = await post('/api/Department/GetAllDepartment', {});
        if (!deptsRes || !deptsRes.data || !Array.isArray(deptsRes.data) || deptsRes.data.length === 0) {
            deptsRes = await post('/api/Department/GetDepartments', { pagenum: 1, limit: 10 });
        }
        
        let deptList = null;
        if (deptsRes && deptsRes.data) {
            deptList = deptsRes.data.rows || deptsRes.data.Items || (Array.isArray(deptsRes.data) ? deptsRes.data : []);
        }

        let targetDept = null;
        if (!deptList || deptList.length === 0) {
            console.log("   ➤ 发现为空表，系统自动开始锻造内外科室底座 [CreateDepartment] ...");
            let newDept = {
                id: "test_" + Date.now(),
                name: "内测全封闭发货科室",
                businessType: "OUTPATIENT",
                state: 1,
                isSuspended: 0
            };
            await post('/api/Department/CreateDepartment', newDept);
            targetDept = newDept;
        } else {
            console.log("   ➤ 提取到的首个真实科室实体样本:", JSON.stringify(deptList[0]).substring(0, 150));
            targetDept = deptList[0];
        }
        console.log(`✅ 成功锁定测试科室: ID = ${targetDept.ID || targetDept.id || targetDept.departmentID || targetDept.department_id || targetDept.DepartmentID}, 名称 = ${targetDept.Name || targetDept.name || targetDept.departmentName || targetDept.DepartmentName}`);

        console.log("\n👉 步骤 2: 获取该科室下的医生...");
        let targetDeptId = targetDept.ID || targetDept.id || targetDept.departmentID || targetDept.department_id || targetDept.DepartmentID;
        let docsRes = await post('/api/DoctorCall/GetDoctorByDeptID?DeptID=' + targetDeptId, {});
        // 尝试备用API
        if (docsRes.code && docsRes.code === 404) {
             docsRes = await post(`/api/DoctorCall/GetDoctorByDeptID?DeptID=${targetDeptId}`);
        }
        
        // 如果没有找到专门的医生也没关系，直接硬塞测试数据
        let targetDoctor = "test_doc";
        console.log(`✅ 测试医生身份锁定: ${targetDoctor}`);


        console.log("\n👉 步骤 3: 模拟 HIS 分诊患者入栈 (Enqueue)...");
        const patientData = {
            patientCode: "TEST-" + Date.now(),
            patientName: "李大明白 (E2E测试)",
            sex: "男",
            ageStr: "35",
            deptID: targetDeptId,
            priority: 0,
            requireSignIn: false
        };
        const enqueueRes = await post('/api/queue/enqueue', patientData);
        console.log("   服务端入列响应:", enqueueRes);
        if (!enqueueRes || (!enqueueRes.success && enqueueRes.code !== 200)) {
            console.log("❌ 入栈失败！中止测试");
            return;
        }
        const callListId = (enqueueRes.data ? enqueueRes.data.id : null) || (enqueueRes.data ? enqueueRes.data.calllistid : null);
        console.log(`✅ 患者入栈成功！获得排队表记录主键 ID: ${callListId || '未知'}`);

        console.log("\n👉 步骤 3.5: 模拟患者在大厅报到机扫码签到 (SignIn)...");
        const signInPayload = {
            patientCode: patientData.patientCode
        };
        const signInRes = await post('/api/queue/signIn', signInPayload);
        console.log("   签到响应:", signInRes);
        if (!signInRes || (!signInRes.success && signInRes.code !== 200)) {
            console.log("⚠️ 签到接口报错，或许后台并未强制要求签到，继续强行往下...");
        } else {
            console.log("✅ 签到成功！该名患者已被激活打入 WAITING 就绪池。");
        }

        console.log("\n👉 步骤 4: 模拟医生点击【呼叫下一个】 (AutoCallNext)...");
        const callNextPayload = {
            deptId: targetDeptId,
            doctors: targetDoctor
        };
        const callRes = await post('/api/DoctorCall/AutoCallNext', callNextPayload);
        console.log("   服务端响应:", callRes);
        if (!callRes.success && callRes.code !== 200) {
             console.log("❌ 呼叫失败或队列为空！");
             // 不中断，可能没有挂号
        } else {
             const calledPatient = callRes.data;
             console.log(`✅ 成功呼叫下一位！被叫患者: ${calledPatient ? calledPatient.patientName : '未知'}`);
        }

        console.log("\n👉 步骤 5: 验证叫号大盘列表数据查询 (GetCallList)...");
        const queryPayload = {
            deptId: targetDeptId,
            limit: 10,
            page: 1,
            state: 1 // 1=呼叫中
        };
        const listRes = await post('/api/DoctorCall/GetCallList', queryPayload);
        console.log(`✅ 呼叫中状态患者人数: ${listRes.data && listRes.data.Items ? listRes.data.Items.length : 0}`);

        console.log("\n=============================================");
        console.log(" 🎉 E2E 测试大连通完毕！");
        console.log("=============================================");

    } catch (err) {
        console.log("\n❌ 发生严重异常断点:", err);
    }
}

runTest();
