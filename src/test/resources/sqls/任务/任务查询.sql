select p.*, isnull(c.c, 0) size from(
	select project.f_name f_projectname, task.* 
	from t_task task 
	left join t_project project on task.f_projectid=project.id 
	where task.f_parentid is null and {condition}
) p left join (
	select f_parentid, COUNT(*) c 
	from t_task
	group by f_parentid
) c on p.id=c.f_parentid
order by p.id desc
