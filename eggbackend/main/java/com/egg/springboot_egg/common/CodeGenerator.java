package com.egg.springboot_egg.common;

import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Scanner;

public class CodeGenerator {

    /**
     * 读取控制台内容
     */
    public static String scanner(String tip) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入" + tip + "：");
        if (scanner.hasNext()) {
            String ipt = scanner.next();
            if (StringUtils.isNotBlank(ipt)) {
                return ipt;
            }
        }
        throw new MybatisPlusException("请输入正确的" + tip + "！");
    }

    public static void main(String[] args) {
        // 1. 代码生成器
        AutoGenerator mpg = new AutoGenerator();

        // 2. 全局配置
        GlobalConfig gc = new GlobalConfig();
        String projectPath = System.getProperty("user.dir");
        gc.setOutputDir(projectPath + "/src/main/java");
        gc.setAuthor("Egg jason");
        gc.setOpen(false);
        gc.setSwagger2(true);          // 保留 Swagger 注解
        gc.setBaseResultMap(true);     // 保留 XML ResultMap
        gc.setBaseColumnList(true);    // 保留 XML columnList
        gc.setServiceName("%sService"); // 去掉 Service 接口前缀 I
        mpg.setGlobalConfig(gc);

        // 3. 数据源配置
        DataSourceConfig dsc = new DataSourceConfig();
        dsc.setUrl("jdbc:mysql://localhost:3306/egg_db?useUnicode=true&characterEncoding=UTF8&useSSL=false");
        dsc.setDriverName("com.mysql.cj.jdbc.Driver");
        dsc.setUsername("root");
        dsc.setPassword("123456");
        mpg.setDataSource(dsc);

        // 4. 包配置
        PackageConfig pc = new PackageConfig();
        pc.setParent("com.egg.springboot_egg")
                .setEntity("entity")
                .setMapper("mapper")
                .setService("service")
                .setServiceImpl("service.impl")
                .setController("controller");
        mpg.setPackageInfo(pc);

        // 5. 模板配置（使用默认模板即可，无需自定义）
        mpg.setTemplateEngine(new FreemarkerTemplateEngine());

        // 6. 策略配置
        StrategyConfig strategy = new StrategyConfig();
        strategy.setNaming(NamingStrategy.underline_to_camel);
        strategy.setColumnNaming(NamingStrategy.underline_to_camel);
        strategy.setEntityLombokModel(true);   // 实体类 Lombok
        strategy.setRestControllerStyle(true);  // Controller REST 风格
        strategy.setInclude(scanner("表名，多个英文逗号分割").split(","));
        mpg.setStrategy(strategy);

        // 7. 执行生成
        mpg.execute();
    }
}
