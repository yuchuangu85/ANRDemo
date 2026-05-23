package com.codemx.anrdemo.anr.catalog

enum class AnrCategory(val displayName: String) {
    UserPerceived("用户可感知"),
    ComponentLifecycle("组件生命周期"),
    ResourcePressure("资源压力"),
    LongRunningAdvanced("高级长耗时"),
    DiagnosticOnly("诊断教学")
}
