package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() {}


    @Before("autoFillPointCut()")
    public void atuoFill(JoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();

        Object[] pointArgs = joinPoint.getArgs();
        if(pointArgs==null || pointArgs.length==0){
            return;
        }
        Object arg = pointArgs[0];
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();
        if(operationType==OperationType.INSERT){
            try {
                Method CREATE_TIME = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME,LocalDateTime.class);
                Method CREATE_USER = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method UPDATE_TIME = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method UPDATE_USER = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                CREATE_TIME.invoke(arg,now);
                CREATE_USER.invoke(arg,currentId);
                UPDATE_TIME.invoke(arg,now);
                UPDATE_USER.invoke(arg,currentId);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("公共填充字段异常{}",e);

            }

        }else if(operationType==OperationType.UPDATE){
            try {
                Method UPDATE_TIME = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method UPDATE_USER = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                UPDATE_TIME.invoke(arg,now);
                UPDATE_USER.invoke(arg,currentId);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("公共填充字段异常{}",e);

            }

        }
    }
}
