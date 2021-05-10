update drug set flowchart =replace(flowchart, 'http://', 'https://') where flowchart is not null;
