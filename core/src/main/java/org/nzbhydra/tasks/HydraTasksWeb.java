/*
 *  (C) Copyright 2017 TheOtherP (theotherp@gmx.de)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.nzbhydra.tasks;

import org.nzbhydra.tasks.HydraTaskScheduler.TaskInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class HydraTasksWeb {

    @Autowired
    private HydraTaskScheduler hydraTaskScheduler;

    @RequestMapping(value = "/internalapi/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured({"ROLE_ADMIN"})
    public List<TaskInformation> getTasks() {
        return hydraTaskScheduler.getTasks();
    }

    @RequestMapping(value = "/internalapi/tasks/{taskName}", method = RequestMethod.PUT)
    @Secured({"ROLE_ADMIN"})
    public List<TaskInformation> runTask(@PathVariable String taskName) {
        hydraTaskScheduler.runNow(taskName);
        return getTasks();
    }

}
