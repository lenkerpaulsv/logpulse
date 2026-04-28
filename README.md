# LogPulse

> A tail-based log aggregator that parses and filters structured logs from multiple services in real time.

---

## Installation

```bash
git clone https://github.com/yourorg/logpulse.git
cd logpulse && mvn clean install
```

---

## Usage

Start aggregating logs from one or more services by pointing LogPulse at their log files:

```bash
java -jar target/logpulse.jar --config config/logpulse.yml
```

Example `logpulse.yml`:

```yaml
sources:
  - name: auth-service
    path: /var/log/auth/app.log
    format: json
  - name: payment-service
    path: /var/log/payment/app.log
    format: json

filters:
  level: ERROR
  fields:
    - timestamp
    - service
    - message

output:
  type: console
```

LogPulse will tail each configured log file, parse structured entries, apply the defined filters, and stream matching output to your chosen destination in real time.

---

## Requirements

- Java 17+
- Maven 3.8+

---

## Contributing

Pull requests are welcome. Please open an issue first to discuss any significant changes.

---

## License

This project is licensed under the [MIT License](LICENSE).