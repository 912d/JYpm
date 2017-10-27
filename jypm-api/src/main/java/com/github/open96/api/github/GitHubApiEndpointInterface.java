package com.github.open96.api.github;

import com.github.open96.api.github.pojo.release.ReleaseJSON;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface GitHubApiEndpointInterface {


    /**
     * @param owner          Owner of the repository.
     * @param repositoryName Name of GitHub repository.
     * @return Latest release from repository.
     */
    @GET("repos/{owner}/{repo}/releases/latest")
    Call<ReleaseJSON> getLatestRelease(@Path("owner") String owner, @Path("repo") String repositoryName);

}
