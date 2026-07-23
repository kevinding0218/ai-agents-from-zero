/**
 * 对应 Python：
 *   AnnotatedTypedDict.py  → Annotated + TypedDict（无运行时校验）
 *   AnnotatedPydantic.py   → Pydantic + Field(ge=0, le=150)（有运行时校验）
 *
 * Python AnnotatedTypedDict.py：
 *   Age = Annotated[int, "年龄，范围0-150"]   # 描述只是元数据，运行时不校验范围
 *   class Person(TypedDict):
 *       name: str
 *       age2: Age
 *   p = Person(name="z3", age=111, age2=188)  # age2=188 不报错，描述被忽略
 *
 * Python AnnotatedPydantic.py：
 *   Age = Annotated[int, Field(ge=0, le=150)]  # Pydantic Field → 运行时校验
 *   class Person(BaseModel):
 *       age2: Age
 *   Person(age2=188)  # 抛 ValidationError（超出 0~150）
 *
 * Java 对应：
 *   TypedDict（无校验）→ Java record 或 普通 class（字段类型即约束）
 *   Pydantic Field（有校验）→ Java Bean Validation（@Min / @Max 注解）
 */

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

@SpringBootApplication
public class AnnotatedAndValidation {
    public static void main(String[] args) {
        SpringApplication.run(AnnotatedAndValidation.class, args);
    }
}

// ─── 对应 Python TypedDict（无运行时校验，只有类型约束）──────────────────────
// Python: class Person(TypedDict): name: str; age: int; age2: Age
// Java record：编译期类型约束，运行时不会校验 age2 是否在 0~150
record PersonTyped(String name, int age, int age2) {}

// ─── 对应 Python Pydantic BaseModel（有运行时校验）──────────────────────────
// Python: class Person(BaseModel):
//             age2: Annotated[int, Field(ge=0, le=150, description="年龄")]
// Java: Bean Validation 注解（@Min / @Max）
class PersonValidated {
    @NotBlank
    public String name;

    public int age;

    @Min(value = 0, message = "年龄不能小于 0")    // 对应 Field(ge=0)
    @Max(value = 150, message = "年龄不能大于 150") // 对应 Field(le=150)
    public int age2;  // Annotated[int, Field(ge=0, le=150, description="年龄")]

    public PersonValidated(String name, int age, int age2) {
        this.name = name;
        this.age = age;
        this.age2 = age2;
    }

    @Override
    public String toString() {
        return "PersonValidated{name='" + name + "', age=" + age + ", age2=" + age2 + "}";
    }
}

@Component
class ValidationRunner implements CommandLineRunner {

    @Autowired
    private Validator validator;  // Spring 内置 Bean Validation

    @Override
    public void run(String... args) {
        // ─── TypedDict 对比（无运行时校验）──────────────────────────────────
        System.out.println("【TypedDict 对应（无运行时校验）】");

        // Python: p = Person(name="z3", age=111, age2=188)  ← 不报错
        PersonTyped p = new PersonTyped("z3", 111, 188);  // age2=188，不报错
        System.out.println("TypedDict 等价（Java record）：" + p);
        System.out.println("→ age2=188 超出描述范围，但运行时不校验（与 Python TypedDict 行为一致）");

        System.out.println("\n" + "=".repeat(50));

        // ─── Pydantic 对比（有运行时校验）──────────────────────────────────
        System.out.println("【Pydantic 对应（有运行时校验）】");

        // Python: Person(name="z3", age=11, age2=188)  → 抛 ValidationError
        // Java:   用 Bean Validation 手动校验，违反 @Max(150) 时产生违规
        PersonValidated pValid = new PersonValidated("z3", 11, 188);
        Set<jakarta.validation.ConstraintViolation<PersonValidated>> violations =
                validator.validate(pValid);

        if (!violations.isEmpty()) {
            // 对应 Python: except ValidationError as e: print(e)
            System.out.println("数据校验失败：");
            violations.forEach(v -> System.out.println("  " + v.getPropertyPath() + ": " + v.getMessage()));
        } else {
            System.out.println("校验通过：" + pValid);
        }

        System.out.println("\n--- 校验通过的例子 ---");
        PersonValidated pOk = new PersonValidated("李四", 25, 30);
        Set<jakarta.validation.ConstraintViolation<PersonValidated>> ok = validator.validate(pOk);
        if (ok.isEmpty()) {
            System.out.println("校验通过：" + pOk);
        }
    }
}

/*
【Java vs Python 对照】

Python Annotated[int, "年龄，范围0-150"]（只是描述元数据）
  ↔ Java 字段类型 int（无范围约束注解）
  两者运行时都不会按范围校验

Python Annotated[int, Field(ge=0, le=150)]（Pydantic 运行时校验）
  ↔ Java @Min(0) @Max(150)（Bean Validation）
  两者都会在"创建对象时"（或调用 validator.validate() 时）做范围检查

Python class Person(TypedDict): ...
  ↔ Java record PersonTyped(String name, int age, int age2)

Python class Person(BaseModel): ...
  ↔ Java class PersonValidated { @Min(0) @Max(150) int age2; }

Python ValidationError（Pydantic 抛出）
  ↔ Java Set<ConstraintViolation>（Bean Validation 返回违规集合）

【为什么 LangChain 用 Annotated？】
LangChain 的 with_structured_output 会把 Annotated 里的描述字符串转成 JSON Schema 里的 description，
帮助模型理解这个字段应该填什么内容（语义描述，不是数值约束）。
Java 等价：BeanOutputConverter 读取字段注释/描述，也转成 JSON Schema。
*/
