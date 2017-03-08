
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
