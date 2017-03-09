既然在[示例3](https://github.com/childe/hangout-filter-statmetric)中,我们已经得到了一些聚合结果,何不更进一步把需要告警的聚合结果通过邮件形式发出来呢?

我们就来写一个mail output plugin.  output plugin是最简单的, 只需要继承BaseOutput并实现一个emit方法就好了.

```
public class Mail extends BaseOutput {
    void emit(Map event) {
        sendmail(event);
    }
}
```

event已经传过来了, 想怎么处理都行.

话不多说了, 具体可以看代码. 下面的配置是一个示例, 如果一个Url的平均响应时间高于10S就发告警出来. 告警内容是当前这个Url的响应时间.

```
inputs:
    - Stdin:
        codec: plain

filters:
    - Grok:
        match:
            - '^%{NOTSPACE:url} %{NUMBER:time}'
    - com.example.filter.StatMetric:
        key: url
        value: time
        windowSize: 10
        add_fields:
            metric_type: true

outputs:
    - com.example.output.Mail:
        if:
          - '<#if metric_type?? && metric_type==true>true</#if>'
          - '<#if (stat.mean>=10)>true</#if>'
        mailhost: mail.corp.com
        from_addr: 'xxx@corp.com'
        to_list: ['xxx@corp.com']
        subject: '页面访问响应时间大于10秒'
        login_user: admin
        login_password: "password"
        format: '${url}响应时间大于10秒: 实际响应时间${stat.mean}秒'
```

多说一句, 如果每个Url的响应时间阈值不一样, 怎么处理好呢? 总不至于写100个if else. 我觉得可以放一个Translate插件, 把每个Url的阈值放在字典里面(字典是可以动态加载的), Translate插件把阈值添加到聚合后的Metric事件中. 最后的mail output的if里面就可以写`<#if ((stat.mean-stat.threshold)>0)>true</#if>`
