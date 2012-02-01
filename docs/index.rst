.. XForms Monkey (A.K.A The Hammer) documentation master file, created by
   sphinx-quickstart on Wed Feb 01 10:26:30 2012.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

Welcome to XForms Monkey (A.K.A The Hammer)'s documentation!
============================================================

.. toctree::
   :maxdepth: 2

   
Usage
-----

To use the xforms monkey, build the project (using eclipse + ant-build) or download the instance_tool.jar file from: https://github.com/downloads/dimagi/XForms-Monkey/instance_tool.jar

Generate Instances
^^^^^^^^^^^^^^^^^^

Then run one of the following commands::

	java -jar instance_tool.jar generate [form_path] [number_of_instances]


This command generates number_of_instances from the form located at form_path (instances are filled with random data)


Send Instances
^^^^^^^^^^^^^^

::

	java -jar instance_tool.jar send [submissions_path] [server_post_url]

Submits all the xml files in submission_path to server_post_url

.. CAUTION::
	The instance_tool only supports basic authentication at this time, login credentials need to be specified in the server_post_url. No password prompting functionality exists at this time.

Generate Answer Key
^^^^^^^^^^^^^^^^^^^

::

	java -jar instance_tool.jar answers [Xform_path] [csv_output_path]

Generates all the possible answers for each question in form Xform_path and outputs to a csv file located at csv_output_path
   


Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`

