#!/usr/bin/python

import sys
import getopt
import os
import yaml
import jinja2


#python test_triforce_template.py -c path/to/context.yaml -t path/to/template.jinja

def error(message):
    """
    Print an error message, usage information, then exit
    """
    print(message)
    usage()
    sys.exit(2)


def usage():
    """
    Print command usage information
    """
    print("Usage: python %s -c contextFile -t templateFile"% sys.argv[0])


def parse_arguments():
    """
    Parse and validate command line arguments 
        - returns a dict of parameters
    """
    try:
        options, args = getopt.getopt(sys.argv[1:], "hc:t:",
            ["help", "context=","template="]
            )
    except getopt.GetoptError as err:
        error(err)
    
    params = {
        'context_file_path' : None,
        'template_file_path' : None,
        }

    for opt, value in options:
        if opt in ("-h", "--help"):
            usage()
            sys.exit()
        elif opt in ("-c", "--context"):
            params['context_file_path'] = value
        elif opt in ("-t", "--template"):
            params['template_file_path'] = value
        else:
            error("Inavlid argument: `%s`"% opt)

    # Validate file paths

    if not params['context_file_path']:
        # Context file path not set
        error("Please provide a context file path")
    elif not os.path.exists(params['context_file_path']):
        error("Context file `%s` not found!"% params['context_file_path'])
    elif not params['template_file_path']:
        error("Please provide a template file path")
    elif not os.path.exists(params['template_file_path']):
        error("Template file `%s` not found!"% params['template_file_path'])
    else:
        return(params)


def parse_context_file(path):
    """
    Loads and parses a yaml file located at 'path'.
    Returns a dict containing the deserialized data
    """
    if not os.path.exists(path):
        raise Exception("No context file found at `%s`"% path)
    else:
        context_file = open(path, 'r')
        context = yaml.load(context_file)
        return(context)


def build_template_environment(template_dir):
    """
    Builds and returns a Jinja2 Template Environment
    """
    tpl_loader = jinja2.FileSystemLoader(template_dir)
    template_env = jinja2.Environment(loader=tpl_loader)
    return(template_env)


def render(template_file_path, context_file_path):
    """
    Render and return a jinja2 template, assuming Triforce spec has been
    followed
    """
    template_dir, template_filename = os.path.split(template_file_path)
    template_environment = build_template_environment(template_dir)
    context = parse_context_file(context_file_path)
    template_context = {}
    # Find the proper template context
    for name, ctx in context.items():
        # Check if the current context def is for the template named in
        # template_filename
        if ctx.get('_tpl', None) == template_filename:
            template_context = ctx

    # Finally, render the template
    template = template_environment.get_template(template_filename)

    return(template.render(template_context))


def main():
    params = parse_arguments()
    output = render(**params)
    print(output)


if __name__ == "__main__":
    main()
