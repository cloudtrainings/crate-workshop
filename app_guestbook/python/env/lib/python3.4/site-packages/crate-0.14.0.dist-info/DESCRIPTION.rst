.. image:: https://cdn.crate.io/web/2.0/img/crate-logo_330x72.png
   :height: 36px
   :width: 165px
   :alt: Crate.IO
   :target: https://crate.io

|

.. image:: https://img.shields.io/travis/crate/crate-python.svg
   :target: https://travis-ci.org/crate/crate-python
   :alt: TravisCI

.. image:: https://img.shields.io/pypi/v/crate.svg
   :target: https://pypi.python.org/pypi/crate/
   :alt: PyPI Version

.. image:: https://img.shields.io/pypi/pyversions/crate.svg
   :target: https://pypi.python.org/pypi/crate/
   :alt: Python Version

.. image:: https://img.shields.io/pypi/dw/crate.svg
    :target: https://pypi.python.org/pypi/crate/
    :alt: PyPI Downloads

.. image:: https://img.shields.io/pypi/wheel/crate.svg
    :target: https://pypi.python.org/pypi/crate/
    :alt: Wheel

.. image:: https://img.shields.io/coveralls/crate/crate-python.svg
    :target: https://coveralls.io/r/crate/crate-python?branch=master
    :alt: Coverage


========
Overview
========

This is the database adapter for the Crate database. Its main feature is a
implementation of the Python `DB API 2.0
<http://www.python.org/dev/peps/pep-0249/>`_ specification.

It also includes support for `SQLAlchemy <http://www.sqlalchemy.org>`_.

To get started take a look at the `documentation <https://crate.io/docs/reference/python/>`_.

Installation
============

Installing via pip
------------------

To install the crate client via `pip <https://pypi.python.org/pypi/pip>`_ use
the following command::

    $ pip install crate

To update use::

    $ pip install -U crate


Are you a Developer?
====================

You can build Crate Python Client on your own with the latest version hosted on
GitHub.
To do so, please refer to ``DEVELOP.rst`` for further information.

Help & Contact
==============

Do you have any questions? Or suggestions? We would be very happy
to help you. So, feel free to swing by our public room on HipChat_.
Or for further information and official contact please
visit `https://crate.io/ <https://crate.io/>`_.

.. _HipChat: https://www.hipchat.com/g7Pc2CYwi

License
=======

Copyright 2013-2014 CRATE Technology GmbH ("Crate")

Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  Crate licenses
this file to you under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.  You may
obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations
under the License.

However, if you have executed another commercial license agreement
with Crate these terms will supersede the license and you may use the
software solely pursuant to the terms of the relevant commercial agreement.

==================
Crate Client Usage
==================

Connect to a Database
=====================

Before we can start we have to import the crate client::

    >>> from crate import client

The client provides a ``connect()`` function which is used to establish a
connection, the first argument is the url of the server to connect to::

    >>> connection = client.connect(crate_host)

Crate is a clustered database providing high availability through replication.
In order for clients to make use of this property it is recommended to specify
all hosts of the cluster. This way if a server does not respond, the request is
automatically routed to the next server::

    >>> invalid_host = 'http://not_responding_host:4200'
    >>> connection = client.connect([invalid_host, crate_host])

If no ``servers`` are given, the default one ``http://127.0.0.1:4200`` is used::

    >>> connection = client.connect()
    >>> connection.client._active_servers
    ['http://127.0.0.1:4200']


If the option ``error_trace`` is set to ``True``, the client will print a whole traceback
if a server error occurs::

    >>> connection = client.connect([crate_host], error_trace=True)

It's possible to define a default timeout value in seconds for all servers
using the optional parameter ``timeout``::

    >>> connection = client.connect([crate_host, invalid_host], timeout=5)

Inserting Data
==============

Before executing any statement a cursor has to be opened to perform
database operations::

    >>> cursor = connection.cursor()
    >>> cursor.execute("""INSERT INTO locations
    ... (name, date, kind, position) VALUES (?, ?, ?, ?)""",
    ...                ('Einstein Cross', '2007-03-11', 'Quasar', 7))

To bulk insert data you can use the `executemany` function::

    >>> cursor.executemany("""INSERT INTO locations
    ... (name, date, kind, position) VALUES (?, ?, ?, ?)""",
    ...                [('Cloverleaf', '2007-03-11', 'Quasar', 7),
    ...                 ('Old Faithful', '2007-03-11', 'Quasar', 7)])
    [{u'rowcount': 1}, {u'rowcount': 1}]

`executemany` returns a list of results for every parameter. Each result
contains a rowcount. If an error occures the rowcount is -2 and the result
may contain an `error_message` depending on the error.

.. note::

    If you are using a crate server version older than 0.42.0 the client
    will execute a single sql statement for every parameter in the parameter
    sequence when you are using executemany. In this case, executemany doesn't
    return any value. To avoid that overhead you can
    use ``execute`` and make use of multiple rows in the INSERT
    statement and provide a list of arguments with the length of
    ``number of inserted records * number of columns``::

        >>> cursor.execute("""INSERT INTO locations
        ... (name, date, kind, position) VALUES (?, ?, ?, ?), (?, ?, ?, ?)""",
        ...                ('Creameries', '2007-03-11', 'Quasar', 7,
        ...                 'Double Quasar', '2007-03-11', 'Quasar', 7))

.. Hidden: refresh locations

    >>> cursor.execute("REFRESH TABLE locations")

Selecting Data
==============

To perform the select operation simply execute the statement on the
open cursor::

    >>> cursor.execute("SELECT name FROM locations where name = ?", ('Algol',))

To retrieve a row we can use one of the cursor's fetch functions (described below).

fetchone()
----------

``fetchone()`` with each call returns the next row from the results::

    >>> result = cursor.fetchone()
    >>> pprint(result)
    [u'Algol']

If no more data is available, an empty result is returned::

    >>> while cursor.fetchone():
    ...     pass
    >>> cursor.fetchone()

fetchmany()
-----------

``fetch_many()`` returns a list of all remaining rows, containing no more than the specified
size of rows::

    >>> cursor.execute("SELECT name FROM locations order by name")
    >>> result = cursor.fetchmany(2)
    >>> pprint(result)
    [[u'Aldebaran'], [u'Algol']]

If a size is not given, the cursor's arraysize, which defaults to '1', determines the number
of rows to be fetched::

    >>> cursor.fetchmany()
    [[u'Allosimanius Syneca']]

It's also possible to change the cursors arraysize to an other value::

    >>> cursor.arraysize = 3
    >>> cursor.fetchmany()
    [[u'Alpha Centauri'], [u'Altair'], [u'Argabuthon']]

fetchall()
----------

``fetchall()`` returns a list of all remaining rows:: 

    >>> cursor.execute("SELECT name FROM locations order by name")
    >>> result = cursor.fetchall()
    >>> pprint(result)
    [['Aldebaran'],
     ['Algol'],
     ['Allosimanius Syneca'],
     ['Alpha Centauri'],
     ['Altair'],
     ['Argabuthon'],
     ['Arkintoofle Minor'],
     ['Bartledan'],
     ['Cloverleaf'],
     ['Creameries'],
     ['Double Quasar'],
     ['Einstein Cross'],
     ['Folfanga'],
     ['Galactic Sector QQ7 Active J Gamma'],
     ['Galaxy'],
     ['North West Ripple'],
     ['Old Faithful'],
     ['Outer Eastern Rim']]

Cursor Description
==================

The ``description`` property of the cursor returns a sequence of 7-item sequences containing the
column name as first parameter. Just the name field is supported, all other fields are 'None'::

    >>> cursor.execute("SELECT * FROM locations order by name")
    >>> result = cursor.fetchone()
    >>> pprint(result)
    [1373932800000,
     None,
     u'Max Quordlepleen claims that the only thing left ...',
     None,
     None,
     u'Star System',
     u'Aldebaran',
     None,
     None,
     1]

    >>> result = cursor.description
    >>> pprint(result)
    ((u'date', None, None, None, None, None, None),
     (u'datetime', None, None, None, None, None, None),
     (u'description', None, None, None, None, None, None),
     (u'details', None, None, None, None, None, None),
     (u'flag', None, None, None, None, None, None),
     (u'kind', None, None, None, None, None, None),
     (u'name', None, None, None, None, None, None),
     (u'nullable_date', None, None, None, None, None, None),
     (u'nullable_datetime', None, None, None, None, None, None),
     (u'position', None, None, None, None, None, None))

Closing the Cursor
==================

The following command closes the cursor::

    >>> cursor.close()

If a cursor is closed, it will be unusable from this point forward.
If any operation is attempted to a closed cursor an ``ProgrammingError`` will be raised.

    >>> cursor.execute("SELECT * FROM locations")
    Traceback (most recent call last):
    ...
    ProgrammingError: Cursor closed

Closing the Connection
======================

The following command closes the connection::

    >>> connection.close()

If a connection is closed, it will be unusable from this point forward.
If any operation using the connection is attempted to a closed connection an ``ProgrammingError``
will be raised::

    >>> cursor.execute("SELECT * FROM locations")
    Traceback (most recent call last):
    ...
    ProgrammingError: Connection closed

    >>> cursor = connection.cursor() 
    Traceback (most recent call last):
    ...
    ProgrammingError: Connection closed

==============
Crate BLOB API
==============

The Crate client library provides an API to access the powerful Blob storage
capabilities of the Crate server.

First, a connection object is required. This can be retrieved by importing the
client module and then connecting to one or more crate server::

    >>> from crate import client
    >>> connection = client.connect(crate_host)

Every table which has Blob support enabled, may act as a container for
Blobs. The ``BlobContainer`` object for a specific table can be
retrieved like this::

    >>> blob_container = connection.get_blob_container('myfiles')
    >>> blob_container
    <BlobContainer 'myfiles'>

The returned container object can now be used to manage the contained
Blobs.

Uploading Blobs
===============

To upload a Blob the ``put`` method can be used. This method takes a
file like object and an optional SHA-1 digest as argument.

In this example we upload a file without specifying the SHA-1 digest::

    >>> from tempfile import TemporaryFile
    >>> f = TemporaryFile()
    >>> _ = f.write(b"this is the content of the file")
    >>> f.flush()

The actual ``put`` - it returns the computed SHA-1 digest upon completion::

    >>> print(blob_container.put(f))
    6d46af79aa5113bd7e6a67fae9ab5228648d3f81

.. note::

  Omitting the SHA-1 digest results in one extra read of the file
  contents to compute the digest before the actual upload
  starts. Therefore, if the application already has a SHA-1 digest for
  the content, or is able to compute the digest on another read
  upfront, providing the digest will lead to better performance.

Here is another example, which provides the digest in the call::

    >>> _ = f.seek(0)
    >>> blob_container.put(f, digest='6d46af79aa5113bd7e6a67fae9ab5228648d3f81')
    False

.. note::

  The above call returned ``False`` because the object already
  existed. Since the digest is already known by the caller and it makes no
  sense to return it again, a boolean gets returned which indicates if
  the Blob was newly created or not.

Retrieving Blobs
================

Retrieving a blob can be done by using the ``get`` method like this::

    >>> res = blob_container.get('6d46af79aa5113bd7e6a67fae9ab5228648d3f81')

The result is a generator object which returns one chunk per iteration::

    >>> print(next(res))
    this is the content of the file

It is also possible to check if a blob exists like this::

    >>> blob_container.exists('6d46af79aa5113bd7e6a67fae9ab5228648d3f81')
    True

Deleting Blobs
==============

To delete a blob just call the ``delete`` method, the resulting boolean
states whether a blob existed under the given digest or not::

    >>> blob_container.delete('6d46af79aa5113bd7e6a67fae9ab5228648d3f81')
    True


