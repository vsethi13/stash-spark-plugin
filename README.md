This plugin implements a git post-receive hook using Atlassian SDK.
The plugin(hook) sends push notifications to a Cisco Spark space when an activity happens on a repository.

Action Items:

- [Done] Capture following events for all the refs that are pushed:
	- [Done] ref creation
	- [Done] ref deletion
	- [Done] ref updation (commits and merge)
	- [Done] commit info
	- [Done] Pull Request activity on that repo
	- [Done] Limit commit info (when it gets huge)

- [Done] Implement a settings UI to get Cisco Spark configuration details (space id)

- [Done] Channel all the notifications to the supplied Cisco Spark space

- [Done] Integrate with CD Spark Notification API to push notifications to a Spark space

- [Done] Explore difference in implementation of different kinds of refs [branch vs. tag]