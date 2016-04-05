# Getting Started with the Crate.IO PHP Sample App
## Get Source

```bash
git clone git@github.com:crate/crate-sample-apps.git
cd php
```

## Load Dependencies
Install the application dependencies with [Composer](https://getcomposer.org/).

```bash
composer install
```

## Install additional software needed - curl

```bash
sudo apt-get autoremove
sudo apt-get install php5-curl
```

## Run PHP app
Start a local web server to test the app

```bash
php -S localhost:8080
```

Open *http://10.11.12.147:8080* in a web browser.
