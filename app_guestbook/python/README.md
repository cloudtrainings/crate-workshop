# Getting Started with the Crate.IO Python Sample App
This Python backend uses [Flask][1] as web framework and the [crate][2] Python library, which is an implementation of the standard [Python DB API][3] (plus [SQLAlchemy][5] dialect).

## Requirements - please make sure python 3.x is installed

- Python 3 (>3.2)
- [virtualenv][4]

## Further installation steps needed:

```bash
sudo apt-get install python3-setuptools
sudo easy_install3 pip
sudo pip3 install virtualenv
```


### Create virtualenv

```bash
source ./env/bin/activate
./env/bin/pip install -r requirements.txt
```

Read _README.txt_ in the root folder of the project for instructions on how to create table schemas and populate sample country data.

### Run Backend Application

```bash
./env/bin/python app.py
```

[1]: http://flask.pocoo.org/
[2]: https://pypi.python.org/pypi/crate
[3]: https://www.python.org/dev/peps/pep-0249/
[4]: https://virtualenv.readthedocs.org/en/latest/
[5]: http://www.sqlalchemy.org/
