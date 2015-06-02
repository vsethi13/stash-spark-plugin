This plugin implements a git post-receive hook using Atlassian SDK.
The plugin(hook) sends push notifications to a Cisco Spark room when someone pushes to a repository.

Action Items:

- [ToDo] Capture following events for all the refs that are pushed:
	- [Done] ref creation
	- [Done] ref deletion
	- [Done] ref updation (commits and merge)
	- [Done] commit info
	- [Done] Pull Request activity on that repo
	- [ToDo] Limit commit info (when it gets huge)

- [Done] Implement a UI to get Cisco Spark configuration details (room name)

- [Done] Channel all the notifications to the supplied Cisco Spark room

- [Done] Integrate with Spark API SDK to push notifications to a Spark room

- [Done] Explore difference in implementation of different kinds of refs [branch vs. tag]