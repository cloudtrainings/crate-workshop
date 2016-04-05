# Frontend

The frontend is written in [AngularJS](https://angularjs.org/) (1.4.8).

The single file `app.js` contains all the application logic.

## Serve application

The simplest way to serve the frontend is by using the Python
[SimpleHTTPServer](https://docs.python.org/2/library/simplehttpserver.html).

Actual version of python used:
```console
$ python --version
Python 3.4.3
$
```

The way to start python web server (for python version >=3):
```console
$ cd frontend
$ python -m http.server
```

Then open the application on port `8000` in the browser:
`http://10.11.12.147:8000/index.html`

Please ensure app.js points to the IP and port where Jetty server is running:
```console
value('apiHost', 'http://10.11.12.147:8080')
```